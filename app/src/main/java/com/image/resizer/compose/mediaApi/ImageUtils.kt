/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface.ORIENTATION_NORMAL
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import apps.sai.com.imageresizer.BuildConfig
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.util.getUri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

val sdcardRegex = "^/storage/[A-Z0-9]+-[A-Z0-9]+/.*$".toRegex()
const val TAG = "HomeScreen"

fun pruneInternalStorage(context: Context) {
    val filesDir = context.filesDir
    try {
        filesDir.listFiles()?.forEach { file ->
            // Example: delete files older than one week
                file.delete()
        }
    } catch (e: IOException) {
        println("Error pruning internal storage: ${e.message}")
    }
}
fun clearCache(context: Context) {
    try {
        val cacheDir = context.cacheDir
        if (cacheDir.isDirectory) {
            cacheDir.listFiles()?.forEach {
                println( " deleting "+it.deleteRecursively())
            }
        }
    } catch (e: Exception) {
        println("Error clearing cache: ${e.message}")
    }
}

@Composable
fun rememberBitmapPainter(bitmap: Bitmap): State<Painter> {
    return remember(bitmap) { derivedStateOf { BitmapPainter(image = bitmap.asImageBitmap()) } }
}

fun FloatArray.to3x3Matrix(): FloatArray {
    return floatArrayOf(
        this[0], this[1], this[2],
        this[5], this[6], this[7],
        this[10], this[11], this[12]
    )
}

fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val aspectRatio = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int

    if (width > height) {
        newWidth = maxWidth
        newHeight = (maxWidth / aspectRatio).toInt()
    } else {
        newHeight = maxHeight
        newWidth = (maxHeight * aspectRatio).toInt()
    }

    return bitmap.scale(newWidth, newHeight)
}

fun overlayBitmaps(currentImage: Bitmap, markupBitmap: Bitmap): Bitmap {
    // Create a new bitmap with the same dimensions as the current image
    val resultBitmap = createBitmap(
        currentImage.width,
        currentImage.height,
        currentImage.config ?: Bitmap.Config.ARGB_8888
    )

    // Create a canvas to draw on the new bitmap
    val canvas = Canvas(resultBitmap)

    // Draw the current image on the canvas
    canvas.drawBitmap(currentImage, 0f, 0f, null)

    // Draw the markup bitmap on top of the current image
    canvas.drawBitmap(markupBitmap.copy(Bitmap.Config.ARGB_8888, true), 0f, 0f, null)

    return resultBitmap
}

fun Bitmap.flipHorizontally(): Bitmap {
    val matrix = Matrix().apply { postScale(-1f, 1f, width / 2f, height / 2f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.flipVertically(): Bitmap {
    val matrix = Matrix().apply { postScale(1f, -1f, width / 2f, height / 2f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun List<Media>.canBeTrashed(): Boolean {
    return find { it.path.matches(sdcardRegex) } == null
}

/**
 * first pair = trashable
 * second pair = non-trashable
 */
fun List<Media>.mediaPair(): Pair<List<Media>, List<Media>> {
    val trashableMedia = ArrayList<Media>()
    val nonTrashableMedia = ArrayList<Media>()
    forEach {
        if (it.path.matches(sdcardRegex)) {
            nonTrashableMedia.add(it)
        } else {
            trashableMedia.add(it)
        }
    }
    return trashableMedia to nonTrashableMedia
}

fun Media.canBeTrashed(): Boolean {
    return !path.matches(sdcardRegex)
}

@Composable
fun rememberActivityResult(onResultCanceled: () -> Unit = {}, onResultOk: () -> Unit = {}) =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == RESULT_OK) onResultOk()
            if (it.resultCode == RESULT_CANCELED) onResultCanceled()
        }
    )


fun <T : Media> T.writeRequest(
    contentResolver: ContentResolver,
) = IntentSenderRequest.Builder(
    MediaStore.createWriteRequest(
        contentResolver,
        arrayListOf(getUri())
    )
)
    .build()

fun <T : Media> List<T>.writeRequest(
    contentResolver: ContentResolver,
) = IntentSenderRequest.Builder(MediaStore.createWriteRequest(contentResolver, map { it.getUri() }))
    .build()

fun Uri.writeRequest(
    contentResolver: ContentResolver,
) = IntentSenderRequest.Builder(MediaStore.createWriteRequest(contentResolver, arrayListOf(this)))
    .build()

fun Uri.authorizedUri(context: Context): Uri = if (this.toString()
        .startsWith("content://")
) this else FileProvider.getUriForFile(
    context,
    BuildConfig.CONTENT_AUTHORITY,
    this.toFile()
)
 suspend fun loadScaledBitmapFromUri(
    context: Context,
    uri: Uri,
    maxWidth: Int? =300,
    maxHeight: Int? =300
): Bitmap?  {
    try {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
        if(maxWidth!=null && maxHeight!=null){
            request.size(Size(maxWidth, maxHeight)) // Specify the desired size
        }


        val result = imageLoader.execute(request.build())

        if (result.drawable != null) {
            // Convert the drawable to a Bitmap
            val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            return bitmap
        } else {
            Log.e("loadBitmapFromUri", "Failed to load bitmap from URI: $uri")
            return null
        }
    } catch (e: Exception) {
        Log.e("loadBitmapFromUri", "Error loading bitmap from URI: $uri", e)
        return null
    }
}

fun loadBitmapFromUri( uri:Uri,context: Context): Bitmap? {
   var originalBitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it)
    }
    return originalBitmap
}

fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}


fun Bitmap.saveBitmapToTempFile(context: Context): Long? {
    // Create a temporary file
    val tempFile: File
    try {
        tempFile = File.createTempFile("temp_bitmap", ".jpg", context.cacheDir)
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
    // Open a FileOutputStream to write to the file
    var fos: FileOutputStream? = null
    try {
        fos = FileOutputStream(tempFile)

        // Compress the bitmap and write it to the file
        compress(Bitmap.CompressFormat.JPEG, 100, fos)

        // Flush the output stream
        fos.flush()
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    } finally {
        // Close the FileOutputStream
        fos?.close()
    }

    // Get the file size
    val fileSize = tempFile.length()
    // Clean up the temporary file
    tempFile.delete()

    // Return the size of the file
    return fileSize
}

// Usage:
// val bitmap = // Load your bitmap here
// val fileSize = saveBitmapToTempFile(context, bitmap)
fun getExifOrientation(context: Context, imageUri: Uri): Int {
    var inputStream: InputStream? = null
    return try {
        inputStream = context.contentResolver.openInputStream(imageUri)
        val exifInterface = ExifInterface(inputStream!!)
        exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error getting EXIF orientation for URI: $imageUri", e)
        ExifInterface.ORIENTATION_NORMAL
    } finally {
        inputStream?.close()
    }
}

fun <T : Media> Context.shareMedia(media: T) {
    val originalUri = media.getUri()
    val uri = if (originalUri.toString()
            .startsWith("content://")
    ) originalUri else FileProvider.getUriForFile(
        this,
        BuildConfig.CONTENT_AUTHORITY,
        originalUri.toFile()
    )

    ShareCompat
        .IntentBuilder(this)
        .setType(media.mimeType)
        .addStream(uri)
        .startChooser()
}

fun <T : Media> Context.shareMedia(mediaList: List<T>) {
    val mimeTypes =
        if (mediaList.find { it.duration != null } != null) {
            if (mediaList.find { it.duration == null } != null) "video/*,image/*" else "video/*"
        } else "image/*"

    val shareCompat = ShareCompat
        .IntentBuilder(this)
        .setType(mimeTypes)
    mediaList.forEach {
        shareCompat.addStream(it.getUri())
    }
    shareCompat.startChooser()
}
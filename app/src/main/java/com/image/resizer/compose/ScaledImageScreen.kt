package com.image.resizer.compose

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.scale
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap


data class ImageItem(
    val uri: Uri,
    var scaledBitmap: Bitmap? = null,
    var originalBitmap: Bitmap? = null,
    val fileSize: Long? = null, // Size in bytes
    val imageName: String? = null,// Original name of the image file
    val imageDimension: Pair<Int, Int>?=null,
    var scaledImageDimension: Pair<Int, Int>?=null,
    var scaledFileSize: Long?=null,

    )


@Preview
@Composable
fun ScaledImageScreenPreview() {
    val uri = "content://media/external/file/25".toUri()
    val imageItems = listOf(
        ImageItem(
            uri = uri,
            scaledBitmap = createBitmap(200, 200),
            originalBitmap = createBitmap(100, 100)
        ),
        ImageItem(
            uri = uri, scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ),
        ImageItem(
            uri = uri, scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ),
        ImageItem(
            uri = uri, scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ),
        ImageItem(
            uri = uri, scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ), ImageItem(
            uri = uri, scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ),
        ImageItem(
            uri = uri, scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        )
    )
    val scaleParams = imageItems.map {
        ScaleParams(
            newHeight = 100,
            newWidth = 100,
        )
    }

    ScaledImageScreen(
        imageItems = imageItems,
        scaleParamsList = scaleParams,
        onSaveClicked = {
            println("Save button clicked")
        }
    )
}
const val TAG = "ScaledImageScreen"

@Composable
fun ScaledImageScreen(
    imageItems: List<ImageItem>,
    scaleParamsList: List<ScaleParams>,
    onSaveClicked: () -> Unit,
) {
    var imagesScaled by remember { mutableStateOf(false) }
    var scaledImages by remember { mutableStateOf(listOf<ImageItem>()) }
    val context = LocalContext.current
    LaunchedEffect(scaleParamsList) {
        if (scaleParamsList.isNotEmpty()) {
            imagesScaled = false
            withContext(Dispatchers.IO) {
                scaleImages(imageItems, scaleParamsList, context) {
                    imagesScaled = true
                    scaledImages = imageItems
                }

            }
        }
    }
    Box(modifier = Modifier
        .fillMaxSize()) {
        Column(
            modifier = Modifier.Companion
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            Log.d(TAG,"ScaledImageScreen $imagesScaled ${scaleParamsList?.size}" )
             if(imagesScaled) {
                 ScaledImagesGrid(
                     modifier = Modifier
                         .fillMaxSize()
                         .padding(bottom = 10.dp), scaledImages, imageItems
                 )
             }else{
                 Text("Scaling...")
             }
        }
        // Save button at the bottom, above the FAB
        if (imagesScaled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp) // Padding around the button
                    .background(Color.LightGray) // Background for better visibility
            ) {
                Button(
                    onClick = {
                        saveImagesToGallery(context, imageItems)
                        onSaveClicked()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Save Scaled Images")
                }
            }
        }
    }
}


@Preview
@Composable
fun ScaledImagesGridPreview() {
    val uri = "content://media/external/file/25".toUri()
    val imageItems = listOf(
        ImageItem(
            uri = uri,
            scaledBitmap = createBitmap(300, 300),
            originalBitmap = createBitmap(100, 200)
        ),
        ImageItem(
            uri = uri, scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ),
        ImageItem(
            uri = uri, scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ),
    )
    ScaledImagesGrid(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 10.dp), scaledImages = imageItems, imageItems
    )
}

@Preview
@Composable
fun GalleryImagesComponentPreview1() {
    val uri = "content://media/external/file/25".toUri()
    val imageItems = listOf(
        ImageItem(
            uri = uri,
            scaledBitmap = createBitmap(300, 300),
            originalBitmap = createBitmap(100, 200)
        ),
        ImageItem(
            uri = uri, scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ),
        ImageItem(
            uri = uri, scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ),
    )
    GalleryImagesComponent(
        imageItems = imageItems
    )
}



@Composable
 fun GalleryImagesComponent(imageItems: List<ImageItem>) {
    val columns = if (imageItems.size > 1) {
        GridCells.Fixed(2)
    } else {
        GridCells.Fixed(1)
    }
    Spacer(modifier = Modifier.height(20.dp))
    LazyVerticalGrid(
        columns = columns,
        contentPadding = PaddingValues(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(imageItems) { imageItem ->
            Column   (
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {

                imageItem.imageDimension?.let {
                    Text(" ${it.first}x${it.second}"  )
                }
                imageItem.fileSize?.let {
                    Text("${it / 1024} kb", maxLines = 1)
                }

                Spacer(modifier = Modifier.height(4.dp))

                AsyncImage(
                    placeholder = painterResource(R.drawable.ic_undo_24dp),
                    model = imageItem.uri,
                    contentDescription = null,
                    modifier = Modifier.Companion
                        .align(Alignment.CenterHorizontally)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .sizeIn(minWidth = 100.dp, minHeight = 200.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

@Composable
internal fun ScaledImagesGrid(
    modifier: Modifier,
    scaledImages: List<ImageItem>,
    imageItems: List<ImageItem>
) {
    if (imageItems.isEmpty()) {
        Text(
            text = "No images selected.",
            textAlign = TextAlign.Center
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(bottom = 5.dp, top = 10.dp, start = 5.dp, end = 5.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = modifier
        ) {
            items(scaledImages.size) { index ->
                val imageItem = scaledImages[index]
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.Gray),
                    horizontalArrangement = Arrangement.SpaceBetween, // Center columns
                    verticalAlignment = Alignment.CenterVertically // Center vertically
                ) {
                    // First Column
                    Column(
                        modifier = Modifier
                            .weight(1f) // Equal weight for both columns
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, // Center image
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {

                        if (imageItem.originalBitmap != null) {
                            Text("Original : ${imageItem.originalBitmap?.width ?: 0}x${imageItem.originalBitmap?.height ?: 0}")
                            imageItem.fileSize?.let {
                                val fileSizeInKb = it / 1024
                                val fileSizeText = if (fileSizeInKb > 1000) {
                                    "${fileSizeInKb / 1024} mb"
                                } else "$fileSizeInKb kb"
                                Text(fileSizeText, maxLines = 1)
                            }
                            Image(
                                bitmap = imageItem.originalBitmap!!.asImageBitmap(),
                                contentDescription = "Scaled Image",
                                modifier = Modifier
                                    .sizeIn(
                                        minWidth = 200.dp,
                                        minHeight = 200.dp,
                                        maxHeight = 300.dp
                                    )
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(1.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = "loading ...",
                                textAlign = TextAlign.Center
                            )
                        }
                    }


                    // Second Column
                    Column(
                        modifier = Modifier
                            .weight(1f) // Equal weight for both columns
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, // Center image
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (imageItem.scaledBitmap != null) {
                            Text("Scaled : ${imageItem.scaledBitmap?.width ?: 0}x${imageItem.scaledBitmap?.height ?: 0}")
                            imageItem.scaledFileSize?.let {
                                val fileSizeInKb = it / 1024
                                val fileSizeText = if (fileSizeInKb > 1000) {
                                    "${fileSizeInKb / 1024} mb"
                                } else "$fileSizeInKb kb"
                                Text(fileSizeText, maxLines = 1)
                            }
                            Image(
                                bitmap = imageItem.scaledBitmap!!.asImageBitmap(),
                                contentDescription = "Scaled Image",
                                modifier = Modifier
                                    .sizeIn(
                                        minWidth = 200.dp,
                                        minHeight = 200.dp,
                                        maxHeight = 300.dp
                                    )
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(1.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = "loading ...",
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}


private  fun scaleImages(
    imageItems: List<ImageItem>,
    scaleParamsList: List<ScaleParams>,
    context: Context,
    onComplete: () -> Unit
) {
    imageItems.forEachIndexed { index, imageItem ->
        val scaleParams = scaleParamsList[index]

        imageItem.originalBitmap = context.contentResolver.openInputStream(imageItem.uri)?.use {
            BitmapFactory.decodeStream(it)
        }

        val scaledWidth =
            scaleParams.newWidth
        val scaledHeight =
            scaleParams.newHeight

        val scaledBitmap = context.contentResolver.openInputStream(imageItem.uri)?.use {
            BitmapFactory.decodeStream(it)?.scale(scaledWidth, scaledHeight, false)
        }
        scaledBitmap?.let {
            val sizeInBytes = BitmapUtils.getBitmapSize(scaledBitmap)
            imageItem.scaledFileSize = sizeInBytes
        }

        imageItem.scaledImageDimension = Pair(scaledWidth,scaledHeight)
        imageItem.scaledBitmap = scaledBitmap
    }
    onComplete()
}

internal fun imageDimensionsFromUri(
    context: Context,
    uri: Uri
): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
    val originalWidth = options.outWidth
    val originalHeight = options.outHeight
    return Pair(originalWidth, originalHeight)
}

internal fun saveImagesToGallery(context: Context, imageItems: List<ImageItem?>,
                                 customDirectoryName: String="ImageResizer") {
    val customDirectoryName = customDirectoryName
    val resolver = context.contentResolver

    imageItems.filterNotNull().forEach { imageItem ->
        imageItem.scaledBitmap?.let { bitmap ->
            val displayName =
                "imageResizer_${
                    SimpleDateFormat(
                        "MMdd_HHmm",
                        Locale.getDefault()
                    ).format(Date())
                }.jpg"
            val mimeType = "image/jpeg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 (Q) and above
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/$customDirectoryName"
                    )
                }

                try {
                    val uri =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    uri?.let {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        }
                    }
                    Log.d("SaveImage", "Image saved to gallery (Q+): $uri")

                } catch (e: IOException) {
                    Log.e("SaveImage", "Error saving image (Q+): ${e.message}")
                }
            } else {
                // For Android versions before 10 (Q)
                val picturesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val customDir = File(picturesDir, customDirectoryName)
                if (!customDir.exists()) {
                    customDir.mkdirs()
                }
                val file = File(customDir, displayName)
                try {
                    FileOutputStream(file).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                    // Make sure the file is visible in the gallery immediately
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(file.toString()),
                        arrayOf(mimeType),
                        null
                    )
                    Log.d("SaveImage", "Image saved to gallery (pre-Q): $file")
                } catch (e: IOException) {
                    Log.e("SaveImage", "Error saving image (pre-Q): ${e.message}")
                }
            }
        }
    }
}


@Composable
fun ImagePreview(imageUri: Uri?) {
    var bitmap: Bitmap? by remember { mutableStateOf(null) }
    val context = LocalContext.current

    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            bitmap = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(imageUri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Selected Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Fit
        )
    }
}
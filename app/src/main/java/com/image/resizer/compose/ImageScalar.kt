package com.image.resizer.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.scale
import com.image.resizer.compose.mediaApi.TAG
import com.image.resizer.compose.mediaApi.loadBitmapFromUri

const val TARGET_FILE_SIZE_KB = 100
private fun createTempFile(context: Context): File {
    val timeStamp: String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}

fun compressImageToTargetSize(
    context: Context,
    imageItem: ImageItem,
    percentOriginal: Int = TARGET_FILE_SIZE_KB
): ImageItem {
    val compressedImageItems = mutableListOf<ImageItem>()

    val imageUri = imageItem.uri
    var bitmap = loadBitmapFromUri(imageUri, context)
    bitmap?.let { it ->
        // Get EXIF orientation
        // bitmap = rotateBitmap(bitmap, exifOrientation)
        var currentFileSizeBytes = imageItem.fileSize
        var scaleFactor = 1f
        val desiredSize = (currentFileSizeBytes * (percentOriginal * .01f)).toLong()
        while (currentFileSizeBytes > desiredSize) {
            scaleFactor *= 0.8f // Decrease scale factor
            val newWidth = (it.width * scaleFactor).toInt()
            val newHeight = (it.height * scaleFactor).toInt()
            if (newWidth <= 10 && newHeight <= 10) {
                break
            }
            try {
                val scaledBitmap = it.scale(newWidth, newHeight)

                // Update file size after scaling
                val tempFile = createTempFile(context)

                FileOutputStream(tempFile).use { outputStream ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                currentFileSizeBytes = (tempFile.length()).toLong()
                bitmap = scaledBitmap // Update bitmap for next iteration
                imageItem.scaledFileSize = currentFileSizeBytes

            } catch (e: Exception) {
                Log.e(TAG, "Error scaling image: $imageUri", e)
                break
            }
        }
        //   bitmap = rotateBitmap(bitmap, exifOrientation)
        // Save the scaled bitmap and add its Uri to the list
        //   if (exifOrientation != ORIENTATION_NORMAL) {
        //     bitmap = rotateBitmap(bitmap, exifOrientation)
        //  }
        // saveBitmapToTempAndGetUri(context, imageItem)
        bitmap?.let {
            val compressedImageItem = imageItem.saveBitmapToTempAndGetUri(context, bitmap)
            bitmap.recycle()
            //  val savedUri = saveImageToGallery(bitmap)
            compressedImageItems.add(compressedImageItem)
            return compressedImageItem
        }


    }
    return imageItem
    // return@withContext compressedImageItems
}

fun ImageItem.scaleImage(
    scaleParams: ScaleParams,
    context: Context,
): ImageItem {
    val imageItem = this
    val scaledWidth =
        scaleParams.newWidth
    val scaledHeight =
        scaleParams.newHeight
    val width = imageDimension.first
    val height = imageDimension.second
    if (scaleParams.scaleFactor != null) {
        val newWidth = (width * scaleParams.scaleFactor).toInt()
        val newHeight = (height * scaleParams.scaleFactor).toInt()
        scaledImageDimension = Pair(newWidth, newHeight)
    }else if (scaleParams.keepAspectRatio) {
        val aspect = width.toFloat() / height.toFloat()
        if (scaledWidth != null) {
            val newHeight = (scaledWidth / aspect).toInt()
            scaledImageDimension = Pair(scaledWidth, newHeight)
        } else if (scaledHeight != null) {
            val newWidth = (scaledHeight * aspect).toInt()
            scaledImageDimension = Pair(newWidth, scaledHeight)
        }
    } else if (scaledWidth != null && scaledHeight != null) {
        scaledImageDimension = Pair(scaledWidth, scaledHeight)
    }

    scaledImageDimension?.let { scaledImageDimension ->

        val scaledBitmap = context.contentResolver.openInputStream(imageItem.uri)?.use {
            BitmapFactory.decodeStream(it)
                ?.scale(scaledImageDimension.first, scaledImageDimension.second, false)
        }
        scaledBitmap?.let {
            val scaledImageItem = imageItem.saveBitmapToTempAndGetUri(context, it)
            scaledBitmap.recycle()
            return scaledImageItem
        }
    }
    return imageItem

}


package com.image.resizer.compose

import android.R.attr.orientation
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
import com.image.resizer.compose.mediaApi.getExifOrientation
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
    val bitmap = loadBitmapFromUri(imageUri, context)
    bitmap?.let {
        // Get EXIF orientation
        // bitmap = rotateBitmap(bitmap, exifOrientation)
        val exif = getExifOrientation(context, imageItem.uri)
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
            val scaledBitmap =bitmap.scale(newWidth, newHeight)
            // Update file size after scaling
            val scaledImageItem = imageItem.saveBitmapToTempAndGetUri(context, scaledBitmap)
            val outputFile = File(context.cacheDir, "${imageItem.imageName}")
            // Set the EXIF data after scaling

            scaledBitmap.recycle()
            /*   return scaledImageItem
               val tempFile = createTempFile(context)

               FileOutputStream(tempFile).use { outputStream ->
                   scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
               }*/
            currentFileSizeBytes = scaledImageItem.scaledFileSize?:0
            if(currentFileSizeBytes<=desiredSize){
             /*   ExifHandler.setExifDataAfterScaling(
                    context = context,
                    originalImageUri = imageItem.uri,
                    scaledBitmap = it,
                    outputFile = outputFile,
                    orientation = exif
                )*/
                println("Scale done  $currentFileSizeBytes")
                return  scaledImageItem
            }

        }
        //   bitmap = rotateBitmap(bitmap, exifOrientation)
        // Save the scaled bitmap and add its Uri to the list
        //   if (exifOrientation != ORIENTATION_NORMAL) {
        //     bitmap = rotateBitmap(bitmap, exifOrientation)
        //  }
        // saveBitmapToTempAndGetUri(context, imageItem)

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
    val exif = getExifOrientation(context, imageItem.uri)
    // Create a new file for the scaled image (or use an existing one)


    if (scaleParams.scaleFactor != null) {
        val newWidth = (width * scaleParams.scaleFactor).toInt()
        val newHeight = (height * scaleParams.scaleFactor).toInt()
        scaledImageDimension = Pair(newWidth, newHeight)
    } else if (scaleParams.keepAspectRatio) {
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
            val scaledImageItem = imageItem.saveBitmapToTempAndGetUri(context, scaledBitmap)
            val outputFile = File(context.cacheDir, "${imageItem.imageName}")
            // Set the EXIF data after scaling
            ExifHandler.setExifDataAfterScaling(
                context = context,
                originalImageUri = imageItem.uri,
                scaledBitmap = scaledBitmap,
                outputFile = outputFile,
                orientation = exif
            )
            scaledBitmap.recycle()
            return scaledImageItem
        }
    }
    return imageItem

}


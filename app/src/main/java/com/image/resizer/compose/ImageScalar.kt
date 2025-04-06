package com.image.resizer.compose

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.scale
import com.image.resizer.compose.mediaApi.getExifOrientation
import com.image.resizer.compose.mediaApi.loadBitmapFromUri

const val TARGET_PERCENTAGE = 100
private fun createTempFile(context: Context): File {
    val timeStamp: String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}

suspend fun compressImageToTargetSize(
    context: Context,
    imageItem: ImageItem,
    percentOriginal: Int = TARGET_PERCENTAGE
): ImageItem {
    val imageUri = imageItem.uri
    val bitmap = loadBitmapFromUri(imageUri, context)
    bitmap?.let {
        checkNotNull(imageItem.originalFileSize){
            "originalFileSize is null in compressImageToTargetSize"
        }
        val exif = getExifOrientation(context, imageItem.uri)
        var currentFileSizeBytes = imageItem.originalFileSize?:0L
        var scaleFactor = 1f
        val desiredSize = (currentFileSizeBytes * (percentOriginal * .01f)).toLong()
        while (currentFileSizeBytes > desiredSize) {
            scaleFactor *= 0.8f // Decrease scale factor
            val newWidth = (it.width * scaleFactor).toInt()
            val newHeight = (it.height * scaleFactor).toInt()
            if (newWidth <= 10 && newHeight <= 10) {
                break
            }
            val scaledBitmap = bitmap.scale(newWidth, newHeight)
            val scaledImageItem = imageItem.saveBitmapToTempAndGetUri(context, scaledBitmap)
            val outputFile = File(context.cacheDir, "${imageItem.imageName}")
            currentFileSizeBytes = scaledImageItem.scaledFileSize ?: 0
            if (currentFileSizeBytes <= desiredSize) {
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

    }
    return imageItem
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

    checkNotNull(originalImageDimension){
        "originalImageDimension is null in scaleImage"
    }

    val width = originalImageDimension?.first?:run {
        log("not able to load dimen")
        1
    }
    val height = originalImageDimension?.second?:1


    val exif = getExifOrientation(context, imageItem.uri)
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


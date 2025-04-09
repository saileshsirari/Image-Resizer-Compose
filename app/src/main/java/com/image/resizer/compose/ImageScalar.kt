package com.image.resizer.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import androidx.core.graphics.scale
import com.image.resizer.compose.mediaApi.getExifOrientation
import io.ktor.http.Url

const val TARGET_PERCENTAGE = 100

suspend fun compressImageToTargetSize(
    context: Context,
    imageItem: ImageItem,
    percentOriginal: Int = TARGET_PERCENTAGE
): ImageItem {
    checkNotNull(imageItem.originalImageDimension == null) {
        "originalImageDimension is null in compressImageToTargetSize"
    }

    checkNotNull(imageItem.originalFileSize == null) {
        "originalFileSize is null in compressImageToTargetSize"
    }
    check((imageItem.originalImageDimension?.first ?: 0) > 10) {
        "originalImageDimension is less than 10 in compressImageToTargetSize"
    }
    check((imageItem.originalImageDimension?.second ?: 0) > 10) {
        "originalImageDimension is less than 10 in compressImageToTargetSize"
    }
    val targetFileSize = (imageItem.originalFileSize?:1).toFloat() * percentOriginal *.01f
    val orgWidth = imageItem.originalImageDimension?.first ?: 10
    val orgHeight = imageItem.originalImageDimension?.second ?: 10
    val desiredWidth = percentOriginal * .01f * orgWidth
    val desiredHeight = percentOriginal * .01f * orgHeight
    val scaledBitmap: Bitmap? = BitmapScaler.decodeSampledBitmapToTargetSize(
        context = context,
        uri =  imageItem.uri,
        targetFileSize =  targetFileSize.toInt(),
        originalWidth = orgWidth,
        originalHeight = orgHeight,
        originalFileSize = (imageItem.originalFileSize?:1).toInt(),
        estimatedBitmapSize = 1,
        imageItem
    )
    val exif = getExifOrientation(context, imageItem.uri)
    scaledBitmap?.let { scaledBitmap ->
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

    checkNotNull(originalImageDimension) {
        "originalImageDimension is null in scaleImage"
    }

    val width = originalImageDimension?.first ?: run {
        log("not able to load dimen")
        1
    }
    val height = originalImageDimension?.second ?: 1


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
        val scaledBitmap: Bitmap? = BitmapScaler.scaleBitmapFromUri(
            context,
            imageItem.uri,
            scaledImageDimension.first.toFloat(),
            scaledImageDimension.second.toFloat(),
            originalWidth = width.toFloat(),
            originalHeight = height.toFloat()
        )
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


package com.image.resizer.compose

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import com.image.resizer.compose.mediaApi.loadBitmapFromUri
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.roundToInt

object BitmapScaler {
    private const val COMPRESSION_QUALITY_START = 100
    private const val COMPRESSION_QUALITY_END = 30
    private const val COMPRESSION_QUALITY_STEP = 5

    /**
     * Loads a scaled-down Bitmap from a content URI, ensuring it's file size
     * is near the targetFileSize.
     *
     * @param context The application context.
     * @param uri The content URI of the image.
     * @param targetFileSize The target file size in bytes.
     * @param originalWidth The width of the original image.
     * @param originalHeight The height of the original image.
     * @param originalFileSize The file size of the original image.
     * @param estimatedBitmapSize The estimated size of the original bitmap.
     * @return The scaled-down Bitmap, or null if the Bitmap could not be loaded.
     */
   suspend fun decodeSampledBitmapToTargetSize(
        context: Context,
        uri: Uri,
        targetFileSize: Int,
        originalWidth: Int,
        originalHeight: Int,
        originalFileSize: Int,
        estimatedBitmapSize: Int,
        imageItem: ImageItem
    ): Bitmap? {
        val contentResolver: ContentResolver = context.contentResolver

        // Start with an estimated inSampleSize
        var inSampleSize = 1

        // Estimate how much we need to scale down
        if (originalFileSize > targetFileSize) {
            // Use a more accurate factor based on file size to calculate the first inSampleSize
            val scaleFactor = originalFileSize.toFloat() / targetFileSize.toFloat()
            inSampleSize = (scaleFactor).roundToInt()
        }

        // Ensure inSampleSize is a power of 2
        inSampleSize = Integer.highestOneBit(inSampleSize)

        // Initial scaling with inSampleSize
        val options = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }

        var scaledBitmap: Bitmap? =
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }


        if (scaledBitmap == null) {

            throw Exception("Unable to scale bitmap")
        }

        // Compress to JPEG to control file size
        var quality = COMPRESSION_QUALITY_START
        var scaledImageItem = imageItem.saveBitmapToTempAndGetUri(context, scaledBitmap,quality)
        checkNotNull(scaledImageItem.scaledFileSize)
        checkNotNull(scaledImageItem.computedUri)
        var compressedSize = scaledImageItem.scaledFileSize?:1
        // Iteratively adjust JPEG quality until we meet the target size
        while (compressedSize > targetFileSize && quality > COMPRESSION_QUALITY_END) {
            quality -= COMPRESSION_QUALITY_STEP
             scaledImageItem = imageItem.saveBitmapToTempAndGetUri(context, scaledBitmap,quality)
            checkNotNull(scaledImageItem.scaledFileSize)
            compressedSize = scaledImageItem.scaledFileSize?:1
        }
        checkNotNull(scaledImageItem.computedUri)
        // Decode the compressed JPEG data back into a Bitmap
        val finalBitmap = loadBitmapFromUri(scaledImageItem.computedUri!!, context)
        scaledBitmap.recycle()
        return finalBitmap
    }

    fun scaleBitmapFromUri(
        context: Context,
        uri: Uri,
        newWidth: Float,
        newHeight: Float,
        originalWidth: Float,
        originalHeight: Float
    ): Bitmap? {
        val contentResolver: ContentResolver = context.contentResolver
        val options = BitmapFactory.Options()

        try {
            val originalBitmap: Bitmap? = contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            originalBitmap?.let { bitmap ->
                val scaleWidth = newWidth / originalWidth
                val scaleHeight = newHeight / originalHeight
                val matrix = Matrix()
                matrix.postScale(scaleWidth, scaleHeight)
                val scaledBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    false
                )
                bitmap.recycle()
                return scaledBitmap
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return null
    }


    /**
     * Scales a Bitmap to fit within the specified maximum width while maintaining the aspect ratio.
     *
     * @param context The application context.
     * @param uri The content URI of the image.
     * @param maxWidth The maximum desired width.
     * @param originalWidth The width of the original image.
     * @param originalHeight The height of the original image.
     * @return The scaled Bitmap, or null if the Bitmap could not be loaded.
     */
    fun scaleBitmapToWidthFromUri(
        context: Context,
        uri: Uri,
        maxWidth: Int,
        originalWidth: Int,
        originalHeight: Int
    ): Bitmap? {

        if (originalWidth <= maxWidth) {
            return context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        }
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        val newHeight = (maxWidth / aspectRatio).toInt()
        return loadScaledBitmap(context, uri, maxWidth, newHeight)
    }

    /**
     * Scales a Bitmap to fit within the specified maximum height while maintaining the aspect ratio.
     *
     * @param context The application context.
     * @param uri The content URI of the image.
     * @param maxHeight The maximum desired height.
     * @param originalWidth The width of the original image.
     * @param originalHeight The height of the original image.
     * @return The scaled Bitmap, or null if the Bitmap could not be loaded.
     */
    fun scaleBitmapToHeightFromUri(
        context: Context,
        uri: Uri,
        maxHeight: Int,
        originalWidth: Int,
        originalHeight: Int
    ): Bitmap? {

        if (originalHeight <= maxHeight) {
            return context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        }

        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        val newWidth = (maxHeight * aspectRatio).toInt()
        return loadScaledBitmap(context, uri, newWidth, maxHeight)
    }
    /**
     * Loads a scaled-down Bitmap from a content URI, ensuring it fits within the
     * specified maximum width and height.
     *
     * @param context The application context.
     * @param uri The content URI of the image.
     * @param reqWidth The requested maximum width of the scaled-down image.
     * @param reqHeight The requested maximum height of the scaled-down image.
     * @param originalWidth The width of the original image.
     * @param originalHeight The height of the original image.
     * @return The scaled-down Bitmap, or null if the Bitmap could not be loaded.
     */
    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int,
        originalWidth: Int,
        originalHeight: Int
    ): Bitmap? {
        val contentResolver: ContentResolver = context.contentResolver

        // Calculate inSampleSize directly using the known dimensions
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(originalWidth, originalHeight, reqWidth, reqHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        try {
            return contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Calculates the inSampleSize value to use for scaling down a Bitmap.
     *
     * @param originalWidth The original image width
     * @param originalHeight The original image height
     * @param reqWidth The requested maximum width.
     * @param reqHeight The requested maximum height.
     * @return The calculated inSampleSize value.
     */
    private fun calculateInSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1

        if (originalHeight > reqHeight || originalWidth > reqWidth) {
            val halfHeight = originalHeight / 2
            val halfWidth = originalWidth / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * use to load the scaled bitmap
     */
    private fun loadScaledBitmap(
        context: Context,
        uri: Uri,
        newWidth: Int,
        newHeight: Int
    ): Bitmap? {
        val contentResolver: ContentResolver = context.contentResolver
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)?.scale(newWidth, newHeight)
        }
    }
}
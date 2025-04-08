package com.image.resizer.compose

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale

object BitmapScaler {

    /**
     * Scales a Bitmap from a URI to fit within the specified maximum width and height while maintaining the aspect ratio.
     *
     * @param context The application context.
     * @param uri The content URI of the image.
     * @param maxWidth The maximum desired width.
     * @param maxHeight The maximum desired height.
     * @param originalWidth The width of the original image.
     * @param originalHeight The height of the original image.
     * @return The scaled Bitmap, or null if the Bitmap could not be loaded.
     */
    fun scaleBitmapFromUri(
        context: Context,
        uri: Uri,
        maxWidth: Float,
        maxHeight: Float,
        originalWidth: Float,
        originalHeight: Float
    ): Bitmap? {

        // If the image is already within the desired dimensions, return it as is
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        }

        val aspectRatio = originalWidth / originalHeight

        var newWidth = maxWidth
        var newHeight = (maxWidth / aspectRatio)

        // Check if scaling to max width exceeds max height
        if (newHeight > maxHeight) {
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio)
        }
        return loadScaledBitmap(context, uri, newWidth.toInt(), newHeight.toInt())
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
        options.inPreferredConfig = Bitmap.Config.RGBA_F16
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)?.scale(newWidth, newHeight)
        }
    }
}
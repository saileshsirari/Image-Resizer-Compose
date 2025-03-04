package com.image.resizer.compose

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi

object BitmapUtils {
    private const val TAG = "BitmapUtils"

    /**
     * Creates a Bitmap from a Content URI.
     *
     * @param context The application context.
     * @param uri The Content URI of the image.
     * @return The Bitmap, or null if an error occurs.
     */
    fun getBitmapFromContentUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val contentResolver: ContentResolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Use ImageDecoder for newer APIs (API 28+)
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                // Use MediaStore.Images.Media.getBitmap for older APIs (deprecated)
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Bitmap from URI: $uri", e)
            null
        }
    }

    fun getBitmapSize(bitmap: Bitmap): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            getBitmapSizeApi12(bitmap)
        } else {
            getBitmapSizePreApi12(bitmap)
        }
    }

    @RequiresApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private fun getBitmapSizeApi12(bitmap: Bitmap): Long {
        return bitmap.byteCount.toLong()
    }

    private fun getBitmapSizePreApi12(bitmap: Bitmap): Long {
        return( bitmap.rowBytes * bitmap.height).toLong()
    }
}
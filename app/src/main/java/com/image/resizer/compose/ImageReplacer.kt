package com.image.resizer.compose

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream

object ImageReplacer {

    fun replaceOriginalImageWithBitmap(context: Context, originalUri: Uri, newBitmap: Bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            replaceImageQAndAbove(context, originalUri, newBitmap)
        } else {
            replaceImageBelowQ(context, originalUri, newBitmap)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun replaceImageQAndAbove(context: Context, originalUri: Uri, newBitmap: Bitmap) {
        try {
            val contentResolver: ContentResolver = context.contentResolver

            // Get the original file's details to maintain metadata (e.g., name, date)
            val projection = arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_MODIFIED
            )
            val cursor = contentResolver.query(originalUri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex =
                        it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val mimeTypeIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                    val dateModifiedIndex =
                        it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

                    val displayName = it.getString(displayNameIndex)
                    val mimeType = it.getString(mimeTypeIndex)
                    val dateModified = it.getLong(dateModifiedIndex)

                    // Update the ContentValues with the original file's metadata
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Images.Media.DATE_MODIFIED, dateModified)
                        put(MediaStore.Images.Media.IS_PENDING, 1) // To prevent immediate scanning
                    }

                    contentResolver.update(originalUri, values, null, null)

                    // Open an OutputStream to write to the file
                    contentResolver.openOutputStream(originalUri)?.use { outputStream ->
                        // Compress and save the new bitmap
                        if (mimeType.contains("png")) {
                            newBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        } else {
                            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        }

                        outputStream.flush()
                    }
                    // Clear the IS_PENDING flag to make it scannable
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(originalUri, values, null, null)

                }
            }
        } catch (e: Exception) {
            Log.e("ImageReplacer", "Error replacing image (Q+): ${e.message}")
            e.printStackTrace()
        }
    }

    private fun replaceImageBelowQ(context: Context, originalUri: Uri, newBitmap: Bitmap) {
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val projection = arrayOf(MediaStore.Images.Media.DATA)

            val cursor = contentResolver.query(originalUri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val dataColumnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val imagePath = it.getString(dataColumnIndex)
                    val file = File(imagePath)

                    // Check if the file exists and is writable
                    if (file.exists() && file.canWrite()) {
                        FileOutputStream(file).use { outputStream ->
                            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                            outputStream.flush()
                        }
                    }
                }
            }
            //Trigger media scanner
            context.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(File(originalUri.path.orEmpty()))
                )
            )
        } catch (e: Exception) {
            Log.e("ImageReplacer", "Error replacing image (Below Q): ${e.message}")
            e.printStackTrace()
        }
    }

    fun getBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("ImageHelper", "Error loading image from URI: ${e.message}")
            null
        }
    }
}
package com.image.resizer.compose

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Log.e
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageScaler(private val context: Context) {

    companion object {
        private const val TAG = "ImageScaler"
        private const val TARGET_FILE_SIZE_KB = 100
    }

    suspend fun scaleImagesToTargetSize(imageUris: List<Uri>,sizeInKb:Int = TARGET_FILE_SIZE_KB): List<Uri?> =
        withContext(Dispatchers.IO) {
            val scaledImageUris = mutableListOf<Uri?>()

            imageUris.forEach { imageUri ->
                var bitmap = loadBitmapFromUri(imageUri) ?: return@forEach
                var currentFileSizeKB = getFileSize(imageUri)
                var scaleFactor = 1.0f

                while (currentFileSizeKB > sizeInKb) {
                    scaleFactor *= 0.9f // Decrease scale factor
                    val newWidth = (bitmap.width * scaleFactor).toInt()
                    val newHeight = (bitmap.height * scaleFactor).toInt()
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

                    // Update file size after scaling
                    val tempFile = createTempFile(context)
                    scaledBitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        100,
                        FileOutputStream(tempFile)
                    )
                    currentFileSizeKB = (tempFile.length() / 1024).toInt()
                    bitmap = scaledBitmap // Update bitmap for next iteration
                }

                // Save the scaled bitmap and add its Uri to the list
                val savedUri = saveImageToGallery(bitmap)
                scaledImageUris.add(savedUri)
            }
            return@withContext scaledImageUris
        }

    private fun loadBitmapFromUri(imageUri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI: $imageUri", e)
            null
        }
    }

    private fun getFileSize(imageUri: Uri): Int {
        return try {
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(imageUri, "r")
            val size = parcelFileDescriptor?.statSize ?: 0
            parcelFileDescriptor?.close()
            (size / 1024).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size for URI: $imageUri", e)
            0
        }
    }

    private fun createTempFile(context: Context): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    fun saveImageToGallery(scaledBitmap: Bitmap,folderName : String=  "ImageResizer"): Uri? {
        val imageName =
            "ImageResizer_Scaled_${
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
                ).format(Date())
            }.jpg"

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, imageName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + File.separator + folderName
                )
            }
        }

        var imageUri: Uri? = null
        var outputStream: OutputStream? = null

        try {
            val insertedUri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (insertedUri != null) {
                imageUri = insertedUri
                outputStream = resolver.openOutputStream(insertedUri)
                if (outputStream != null) {
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    Log.d(TAG, "Image saved to gallery: $imageUri")
                } else {
                    Log.e(TAG, "Error: Failed to open output stream for: $insertedUri")
                }
            } else {
                Log.e(TAG, "Error: Failed to insert image into MediaStore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to gallery: ", e)
        } finally {
            outputStream?.close()
        }

        return imageUri
    }
}
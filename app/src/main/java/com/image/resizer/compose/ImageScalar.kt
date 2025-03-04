package com.image.resizer.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageScalar(private val context: Context) {

    companion object {
        private const val TAG = "ImageScaler"
        private const val TARGET_FILE_SIZE_KB = 100
    }

    suspend fun compressImagesToTargetSize(imageItems: List<ImageItem>, sizeInKb:Int = TARGET_FILE_SIZE_KB): List<ImageItem?> =
        withContext(Dispatchers.IO) {
            val compressedImageUris = mutableListOf<ImageItem?>()
            imageItems.forEach { imageItem ->
                val imageUri = imageItem.uri
                var bitmap = loadBitmapFromUri(imageUri) ?: return@forEach
                imageItem.originalBitmap =  bitmap.config?.let { bitmap.copy(it, true)}
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
                imageItem.scaledBitmap = bitmap
              //  val savedUri = saveImageToGallery(bitmap)
                compressedImageUris.add(imageItem)
            }
            return@withContext compressedImageUris
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

}
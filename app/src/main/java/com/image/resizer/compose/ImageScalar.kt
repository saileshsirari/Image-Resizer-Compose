package com.image.resizer.compose

import android.R.attr.orientation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface.ORIENTATION_NORMAL
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.graphics.createBitmap
import com.image.resizer.compose.mediaApi.getExifOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.scale
import com.image.resizer.compose.mediaApi.loadBitmapFromUri
import com.image.resizer.compose.mediaApi.rotateBitmap
import com.image.resizer.compose.mediaApi.saveBitmapToTempFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

const val TARGET_FILE_SIZE_KB = 100

class ImageScalar() {

    companion object {
        private const val TAG = "ImageScaler"

    }

    fun scaleImages(
        imageItems: List<ImageItem>,
        scaleParamsList: List<ScaleParams>,
        context: Context,
    ): Flow<ImageItem>  =
        flow {
            imageItems.forEachIndexed { index, imageItem ->
                val scaleParams = scaleParamsList[index]


                val scaledWidth =
                    scaleParams.newWidth
                val scaledHeight =
                    scaleParams.newHeight

                val scaledBitmap = context.contentResolver.openInputStream(imageItem.uri)?.use {
                    BitmapFactory.decodeStream(it)?.scale(scaledWidth, scaledHeight, false)
                }

                scaledBitmap?.let {
                    val scaledImageItem = imageItem.saveBitmapToTempAndGetUri(context,it)
                    emit(scaledImageItem)
                }
                scaledBitmap?.recycle()

            }
        }

    fun compressImagesToTargetSize(
        context: Context,
        imageItems: List<ImageItem>,
        percentOriginal: Int = TARGET_FILE_SIZE_KB
    ): Flow<ImageItem> =
        flow {
            val compressedImageItems = mutableListOf<ImageItem>()
            imageItems.forEach { imageItem ->
                val imageUri = imageItem.uri
                var bitmap = loadBitmapFromUri(context, imageUri)
                bitmap?.let {it->

                    // Get EXIF orientation

                    // bitmap = rotateBitmap(bitmap, exifOrientation)
                    var currentFileSizeBytes = imageItem.fileSize
                    var scaleFactor = 1f
                    val desiredSize = (currentFileSizeBytes * (percentOriginal *.01f)).toLong()
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
                        emit(compressedImageItem)
                    }

                }
            }

            // return@withContext compressedImageItems
        }

    private fun loadBitmapFromUri(context: Context, imageUri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI: $imageUri", e)
            null
        }
    }

    private fun getFileSize(context: Context, imageUri: Uri): Long {
        return try {
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(imageUri, "r")
            val size = parcelFileDescriptor?.statSize ?: 0
            parcelFileDescriptor?.close()
            size
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
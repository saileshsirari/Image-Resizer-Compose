package com.image.resizer.compose

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.image.resizer.compose.mediaApi.SaveFormat
import com.image.resizer.compose.mediaApi.saveImage
import com.image.resizer.compose.mediaApi.util.Constants.CUSTOM_FOLDER_NAME
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import java.util.concurrent.CancellationException

class ImageSaveWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    private val contentResolver: ContentResolver = context.contentResolver
    val exceptionHandler = CoroutineExceptionHandler { _, e ->
        println("[ERROR] ${e.message}")
    }
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO+exceptionHandler) {
            try {
                val imageUris = inputData.getStringArray(KEY_IMAGE_URIS)?.map { Uri.parse(it) }
                val saveFormatString = inputData.getString(KEY_SAVE_FORMAT)
                val saveFormat = if(saveFormatString == "PNG" || saveFormatString == "png"){
                    SaveFormat.PNG
                }else{
                    SaveFormat.JPEG
                }

                if (imageUris.isNullOrEmpty()) {
                    Log.e(TAG, "No image URIs provided")
                    return@withContext Result.failure()
                }

                val processed = mutableListOf<ImageItem>()

                imageUris.forEach { uri ->
                    if (isStopped) {
                        throw CancellationException()
                    }

                    var bitmap: Bitmap? = null
                    bitmap = loadBitmapFromUri(uri, context)

                    bitmap?.let {
                        //Save image
                        if (contentResolver.saveImage(
                                uri,
                                context = context,
                                bitmap = it,
                                format = saveFormat.format,
                                relativePath = Environment.DIRECTORY_PICTURES + "/" + CUSTOM_FOLDER_NAME,
                                displayName = "imageResizer_${
                                    processed.size
                                }.jpg",
                                mimeType = saveFormat.mimeType
                            ) == null
                        ) {
                            log(TAG, "mediaHandler.saveImage failed for $uri")
                        } else {
                            processed.add(ImageItem( uri = uri))
                        }
                    } ?: run {
                        log(TAG, "null bitmap for $uri")
                    }

                }


                //  Return success
                val outputData =
                    workDataOf(KEY_SUCCESS to true, KEY_PROCESSED_COUNT to processed.size)
                return@withContext Result.success(outputData)
            } catch (e: CancellationException) {
                Log.e(TAG, "Work cancelled", e)
                return@withContext Result.failure()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving images", e)
                return@withContext Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "ImageSaveWorker"
        const val KEY_IMAGE_URIS = "image_uris"
        const val KEY_SAVE_FORMAT = "save_format"
        const val KEY_SUCCESS = "success"
        const val KEY_PROCESSED_COUNT = "processed_count"
    }

    private fun loadBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun ContentResolver.saveImage1(
        originalUri: Uri,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        relativePath: String = "",
        displayName: String,
        mimeType: String
    ): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1) // Mark as pending
            }

            return runCatching {
                val uri = insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Failed to create new MediaStore record.")

                openOutputStream(uri)?.use { stream ->
                    if (!bitmap.compress(format, 100, stream)) {
                        throw IOException("Failed to save bitmap.")
                    } else {
                        ExifHandler.setExifDataAfterScalingWithUri(
                            context = context,
                            originalImageUri = originalUri,
                            scaledBitmap = bitmap,
                            outputUri = uri,
                        )
                    }
                } ?: throw IOException("Failed to open output stream.")

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0) // Mark as not pending
                update(uri, values, null, null)
                uri
            }.getOrElse {
                it.printStackTrace()
                null
            }
        }
        return null
    }
}
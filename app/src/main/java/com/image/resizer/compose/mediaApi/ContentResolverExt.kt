/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.FileUtils
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.util.Constants
import com.image.resizer.compose.mediaApi.util.Constants.CUSTOM_FOLDER_NAME
import com.image.resizer.compose.mediaApi.util.getUri
import com.image.resizer.compose.mediaApi.util.isVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun ContentResolver.queryFlow(
    uri: Uri,
    projection: Array<String>? = null,
    queryArgs: Bundle? = Bundle(),
) = callbackFlow {
    // Each query will have its own cancellationSignal.
    // Before running any new query the old cancellationSignal must be cancelled
    // to ensure the currently running query gets interrupted so that we don't
    // send data across the channel if we know we received a newer set of data.
    var cancellationSignal = CancellationSignal()
    // ContentObserver.onChange can be called concurrently so make sure
    // access to the cancellationSignal is synchronized.
    val mutex = Mutex()

    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            launch(Dispatchers.IO) {
                mutex.withLock {
                    cancellationSignal.cancel()
                    cancellationSignal = CancellationSignal()
                }
                runCatching {
                    trySend(query(uri, projection, queryArgs, cancellationSignal))
                }
            }
        }
    }

    registerContentObserver(uri, true, observer)

    // The first set of values must always be generated and cannot (shouldn't) be cancelled.
    launch(Dispatchers.IO) {
        runCatching {
            trySend(
                query(uri, projection, queryArgs, null)
            )
        }
    }

    awaitClose {
        // Stop receiving content changes.
        unregisterContentObserver(observer)
        // Cancel any possibly running query.
        cancellationSignal.cancel()
    }
}.conflate()

suspend fun <T : Media> ContentResolver.copyMedia(
    from: T,
    path: String
) = withContext(Dispatchers.IO) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, from.label)
        put(MediaStore.MediaColumns.MIME_TYPE, from.mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, path)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val volumeUri =
        if (from.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    try {
        val outUri = insert(volumeUri, contentValues)
        if (outUri != null) {
            async {
                openFileDescriptor(outUri, "w", null).use { target ->
                    openFileDescriptor(from.getUri(), "r").use { from ->
                        if (target != null && from != null) {
                            try {
                                FileUtils.copy(from.fileDescriptor, target.fileDescriptor)
                            } catch (e: IOException) {
                                if (e.message.toString().contains("ENOSPC")) {
                                    Log.e(Constants.TAG, "No space left on device")
                                } else {
                                    Log.e(Constants.TAG, e.message.toString())
                                }
                                return@async
                            }
                        }
                    }
                }
            }.await()
            val updatedValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
                put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis())
            }

            update(
                outUri,
                updatedValues,
                null
            ) > 0
        } else false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun replaceImageBelowQ(context: Context, originalUri: Uri, newBitmap: Bitmap): Boolean {
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
    return true
}

fun ContentResolver.overrideImage(
    context: Context,
    uri: Uri,
    bitmap: Bitmap,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
): Boolean {

    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis())
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return runCatching {
            update(uri, values, null)
            openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(format, 100, stream))
                    throw IOException("Failed to save bitmap.")
            } ?: throw IOException("Failed to open output stream.")
            update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null
            ) > 0
        }.getOrElse {
            throw it
        }
    } else {
        return runCatching {
            replaceImageBelowQ(context, uri, bitmap)
        }.getOrElse {
            throw it
        }
    }
}


fun ContentResolver.saveImage(
    context: Context,
    bitmap: Bitmap,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    mimeType: String = "image/jpeg",
    relativePath: String = Environment.DIRECTORY_PICTURES,
    displayName: String
): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            relativePath
        )
    }

    var uri: Uri? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

        return runCatching {
            insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also {
                uri = it // Keep uri reference so it can be removed on failure

                openOutputStream(it)?.use { stream ->
                    if (!bitmap.compress(format, 100, stream))
                        throw IOException("Failed to save bitmap.")
                } ?: throw IOException("Failed to open output stream.")


            } ?: throw IOException("Failed to create new MediaStore record.")
        }.getOrElse {
            uri?.let { orphanUri ->
                // Don't leave an orphan entry in the MediaStore
                delete(orphanUri, null, null)
            }

            return null
        }
    } else {

        // For Android versions before 10 (Q)
        return runCatching {
            val customDir = File(relativePath)
            if (!customDir.exists()) {
                customDir.mkdirs()
            }
            val file = File(customDir, displayName)
            FileOutputStream(file).use { outputStream ->
                if (mimeType.contains("png")) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }
            // Make sure the file is visible in the gallery immediately
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.toString()),
                arrayOf(mimeType),
                null
            )
            Log.d("SaveImage", "Image saved to gallery (pre-Q): $file")
            return null
        }.getOrElse {
            return null
        }
    }
}

fun ContentResolver.saveVideo(
    bytes: ByteArray,
    mimeType: String,
    relativePath: String = Environment.DIRECTORY_MOVIES,
    displayName: String
): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            if (relativePath.contains("DCIM") || relativePath.contains("Movies")) relativePath
            else Environment.DIRECTORY_MOVIES + "/Edited"
        )
    }

    var uri: Uri? = null

    return runCatching {
        insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also {
            uri = it // Keep uri reference so it can be removed on failure

            openOutputStream(it)?.use { stream ->
                stream.write(bytes)
            } ?: throw IOException("Failed to open output stream.")

        } ?: throw IOException("Failed to create new MediaStore record.")
    }.getOrElse {
        uri?.let { orphanUri ->
            // Don't leave an orphan entry in the MediaStore
            delete(orphanUri, null, null)
        }

        return null
    }
}


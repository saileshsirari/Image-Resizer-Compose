package com.image.resizer.compose

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.image.resizer.compose.mediaApi.model.Media.UriMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

@Stable
data class ImageItem(
    val key: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val imageName: String? = null,// Original name of the image file
    var scaledImageDimension: Pair<Int, Int>? = null,
    var scaledFileSize: Long? = null,
    var computedUri: Uri? = null,
    var originalFileSize: Long? = null,
    var originalImageDimension: Pair<Int, Int>? = null,
    val originalRelativePath: String,
    val originalMimeType: String,
    val   timestamp: Long,
    val path : String ="" ,
) {
    val uriMutex: Mutex = UriMutexManager.getMutex(uri)

    var scaledUri by mutableStateOf<Uri?>(null)
        private set
    var fileSize by mutableStateOf<Long?>(null)
        private set
    var imageDimension by mutableStateOf<Pair<Int, Int>?>(null)
        private set


    suspend fun computeScaledUri(context: Context): Uri? {
        return withContext(Dispatchers.IO) {
            uriMutex .withLock {
                if (scaledUri == null) {
                    if(originalFileSize ==null){
                        computeFileSize(context)
                    }
                    if(originalImageDimension ==null){
                        computeImageDimension(context)
                    }
                    val imageItem = if (percentScale != null) {
                        computeScaledUriBySize(context)
                    } else if (scaleParams != null) {
                        computeScaledUriByScale(context)
                    } else {
                        this@ImageItem
                    }

                    scaledImageDimension = imageItem.scaledImageDimension
                    scaledFileSize = imageItem.scaledFileSize
                    computedUri = imageItem.computedUri
                    scaledUri = imageItem.computedUri
                }
            }
            this@ImageItem.scaledUri
        }
    }

    internal suspend fun computeImageDimension(context: Context): Pair<Int, Int>? {
        return withContext(Dispatchers.IO) {
            if (originalImageDimension != null) {
                imageDimension = originalImageDimension
                return@withContext originalImageDimension
            }
            imageDimension = imageDimensionsFromUri(context, uri)
            originalImageDimension = imageDimension
            imageDimension
        }
    }
    suspend fun computeOriginalImageDetails(context: Context) {
        withContext(Dispatchers.IO) {
            // Compute file size
            if (originalFileSize != null) {
                this@ImageItem.fileSize = originalFileSize
            }else {
                with(context.contentResolver.openFileDescriptor(uri, "r")) {
                    val size = this?.statSize ?: 0
                    this?.close()
                    this@ImageItem.originalFileSize = size
                    this@ImageItem.fileSize = size
                }
            }

            // Compute image dimensions
            if (originalImageDimension != null) {
                imageDimension = originalImageDimension
            }else {
                imageDimension = imageDimensionsFromUri(context, uri)
                originalImageDimension = imageDimension
            }
        }
    }
    internal suspend fun computeFileSize(context: Context): Long? {
        return withContext(Dispatchers.IO) {
            if (originalFileSize != null) {
                this@ImageItem.fileSize = originalFileSize
                return@withContext originalFileSize
            }
            with(context.contentResolver.openFileDescriptor(uri, "r")) {
                val size = this?.statSize ?: 0
                this?.close()
                this@ImageItem.originalFileSize = size
                this@ImageItem.fileSize = size
                return@withContext size
            }
        }
    }

    internal fun computeScaledUriByScale(context: Context): ImageItem {
        scaleParams?.let {
            val imageItem = scaleImage(it, context)
            return imageItem
        }
        return this
    }

    internal suspend fun computeScaledUriBySize(context: Context): ImageItem {
        percentScale?.let {
            val imageItem = compressImageToTargetSize(context, this, it)
            return imageItem
        }
        return this
    }

    companion object {
        var percentScale: Int? = null
        var scaleParams: ScaleParams? = null
        val mutex = Mutex()

    }

}

fun UriMedia.toImageItem(): ImageItem {
    return ImageItem(
        key = key,
        uri = uri,
        imageName = label,
        originalFileSize = size,
        originalRelativePath = relativePath,
        originalMimeType =  mimeType,
        timestamp = timestamp,
        path = path
    )
}


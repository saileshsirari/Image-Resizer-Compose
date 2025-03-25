package com.image.resizer.compose

import android.content.Context
import android.net.Uri
import java.util.UUID


data class ImageItem(
    val context: Context,
    val key: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val imageName: String? = null,// Original name of the image file
    var scaledImageDimension: Pair<Int, Int>? = null,
    var scaledFileSize: Long? = null,
    var computedUri: Uri? = null,

) {

    val scaledUri: Uri? by lazy {
        val imageItem = if (percentScale != null) {
            computeScaledUriBySize()
        } else if (scaleParams != null) {
            computeScaledUriByScale()
        } else {
            this
        }
        this.scaledImageDimension = imageItem.scaledImageDimension
        this.scaledFileSize = imageItem.scaledFileSize
        this.computedUri = imageItem.computedUri
        imageItem.computedUri
    }

    val fileSize: Long by lazy {
        with(context.contentResolver.openFileDescriptor(uri, "r")) {
            val size = this?.statSize ?: 0
            this?.close()
            size
        }
    }

    val imageDimension: Pair<Int, Int> by lazy {
        imageDimensionsFromUri(context, uri)
    }

    fun computeScaledUriByScale(): ImageItem {
        scaleParams?.let {
            val imageItem = scaleImage(it, context)
            return imageItem
        }
        return this
    }

    fun computeScaledUriBySize(): ImageItem {
        percentScale?.let {
            val imageItem = compressImageToTargetSize(context, this, it)
            return imageItem
        }
        return this
    }

    companion object {
        var percentScale: Int? = null
        var scaleParams: ScaleParams? = null

    }

}


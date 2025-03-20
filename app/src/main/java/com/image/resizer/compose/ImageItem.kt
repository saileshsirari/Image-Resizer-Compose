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
    var scaledUri: Uri? = null,

    ) {

    /* suspend fun loadBitmap(maxWidth: Int? =300,
                           maxHeight: Int? =300): Bitmap?  {
        return loadScaledBitmapFromUri( context,uri ,maxWidth,maxHeight) ?: createBitmap(100, 200)
    }*/


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
}
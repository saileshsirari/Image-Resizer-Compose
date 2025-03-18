package com.image.resizer.compose

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.createBitmap
import com.image.resizer.compose.mediaApi.loadBitmapFromUri
import java.util.UUID


data class ImageItem(
    val context: Context,
    val key: String = UUID.randomUUID().toString(),
    val uri: Uri,
    var scaledBitmap: Bitmap? = null,
    val imageName: String? = null,// Original name of the image file
     var scaledImageDimension: Pair<Int, Int>? = null,
    var scaledFileSize: Long? = null,
    var scaledUri: Uri? = null,

){
    val originalBitmap: Bitmap by lazy {
        loadBitmapFromUri(uri, context) ?: createBitmap(100, 200)
    }

    val fileSize: Long by lazy {
          with(context.contentResolver.openFileDescriptor(uri, "r")) {
              val size =  this?.statSize ?: 0
              this?.close()
              size
          }
    }
    val imageDimension: Pair<Int, Int> by lazy {
        originalBitmap.width to originalBitmap.height}

}
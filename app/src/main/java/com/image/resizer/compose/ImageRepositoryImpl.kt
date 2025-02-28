package com.image.resizer.compose

import android.content.Context
import android.net.Uri
import com.image.resizer.compose.getRealCompressedImageUris
import com.image.resizer.compose.getTotalCompressedImagesCount
import kotlinx.coroutines.flow.Flow

class ImageRepositoryImpl(private val context: Context) : ImageRepository {
    override fun getTotalCompressedImagesCount(): Int {
        return getTotalCompressedImagesCount(context)
    }

    override suspend fun getRealCompressedImageUris(imageIndexes: List<Int>): List<Uri?> {
        return getRealCompressedImageUris(context, imageIndexes)
    }

    override fun deleteImages(images: List<Uri>) {

    }

    override fun getImages(): List<Uri?> {
        TODO("Not yet implemented")
    }

}
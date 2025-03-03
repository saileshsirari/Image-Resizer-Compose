package com.image.resizer.compose

import android.content.Context
import android.net.Uri

class ImageRepositoryImpl(private val context: Context,val imageHelper: ImageHelper) : ImageRepository {
    override fun getTotalCompressedImagesCount(): Int {
        return getTotalTransformedImagesCount(context)
    }

    override suspend fun getRealCompressedImageUris(imageIndexes: List<Int>): List<Uri?> {
        return imageHelper.getRealCompressedImageUris(context, imageIndexes)
    }

    override fun deleteImages(images: List<Uri>) {

    }

    override fun getImages(): List<Uri?> {
        TODO("Not yet implemented")
    }

}
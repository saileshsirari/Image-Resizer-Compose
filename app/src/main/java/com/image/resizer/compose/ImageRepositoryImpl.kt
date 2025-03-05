package com.image.resizer.compose

import android.content.Context
import android.net.Uri

class ImageRepositoryImpl(private val context: Context,val imageHelper: ImageHelper) : ImageRepository {
    override fun getTotalCompressedImagesCount(): Int {
        return getTotalTransformedImagesCount(context)
    }

    override suspend fun getRealCompressedImageUris(imageIndexes: List<Int>): List<ImageItem?> {
        return imageHelper.getRealCompressedImageUris(context, imageIndexes)
    }

    override fun deleteImages(images: List<ImageItem>) {

    }

    override fun getImages(): List<ImageItem?> {
        TODO("Not yet implemented")
    }

}
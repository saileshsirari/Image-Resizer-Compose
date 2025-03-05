package com.image.resizer.compose

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface ImageRepository {
    fun getTotalCompressedImagesCount(): Int
    suspend fun getRealCompressedImageUris(imageIndexes: List<Int>): List<ImageItem?>
    fun deleteImages(images: List<ImageItem>)
    fun getImages(): List<ImageItem?>
}
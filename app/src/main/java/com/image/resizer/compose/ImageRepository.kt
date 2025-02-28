package com.image.resizer.compose

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface ImageRepository {
    fun getTotalCompressedImagesCount(): Int
    suspend fun getRealCompressedImageUris(imageIndexes: List<Int>): List<Uri?>
    fun deleteImages(images: List<Uri>)
    fun getImages(): List<Uri?>
}
/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.image.resizer.compose.mediaApi.model.ExifAttributes
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.util.mediaPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class MediaHandleUseCase(
    private val repository: MediaRepository,
    private val context: Context
) {



    suspend fun <T: Media> trashMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<T>,
        trash: Boolean = true
    ) = withContext(Dispatchers.Default) {
        val isTrashEnabled =  true
        /**
         * Trash media only if user enabled the Trash Can
         * Or if user wants to remove existing items from the trash
         * */
        if ((isTrashEnabled || !trash)) {
            val pair = mediaList.mediaPair()
            if (pair.first.isNotEmpty()) {
                repository.trashMedia(result, mediaList, trash)
            }
            if (pair.second.isNotEmpty()) {
                repository.deleteMedia(result, mediaList)
            }
        } else {
            repository.deleteMedia(result, mediaList)
        }
    }


    suspend fun <T: Media> deleteMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<T>
    ) = repository.deleteMedia(result, mediaList)



    suspend fun <T: Media> updateMediaExif(
        media: T,
        exifAttributes: ExifAttributes
    ): Boolean = repository.updateMediaExif(media, exifAttributes)

    fun saveImage(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ) = repository.saveImage(bitmap, format, mimeType, relativePath, displayName)

    suspend fun getCategoryForMediaId(mediaId: Long) = repository.getCategoryForMediaId(mediaId)

    fun overrideImage(
        uri: Uri,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ) = repository.overrideImage(uri, bitmap, format, mimeType, relativePath, displayName)


}
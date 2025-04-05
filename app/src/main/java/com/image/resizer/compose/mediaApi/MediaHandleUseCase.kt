/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.util.mediaPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaHandleUseCase(
    private val repository: MediaRepository
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


   suspend fun saveImage(
        originalUri: Uri,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ) : Uri?{
     return  withContext(Dispatchers.IO){
         try {
              repository.saveImage(originalUri, bitmap, format, mimeType, relativePath, displayName)
         }catch (e: Exception){
             null
         }
       }
   }

    fun overrideImage(
        originalUri: Uri,
        uri: Uri,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat
    ) = repository.overrideImage(originalUri,uri, bitmap, format,)


}
/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.image.resizer.compose.mediaApi.model.Album
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.model.Media.UriMedia
import com.image.resizer.compose.mediaApi.model.MediaOrder
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMedia(): Flow<Resource<List<UriMedia>>>
    fun getAlbums(mediaOrder: MediaOrder): Flow<Resource<List<Album>>>

    fun getMediaByAlbumIdWithType(
        albumId: Long,
        allowedMedia: AllowedMedia
    ): Flow<Resource<List<UriMedia>>>
    fun getMediaByType(allowedMedia: AllowedMedia): Flow<Resource<List<UriMedia>>>
    fun getAlbumsWithType(allowedMedia: AllowedMedia): Flow<Resource<List<Album>>>
    fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<UriMedia>>>
    suspend fun getCategoryForMediaId(mediaId: Long): String?
    fun getMediaListByUris(listOfUris: List<Uri>, reviewMode: Boolean): Flow<Resource<List<UriMedia>>>

    suspend fun <T: Media> trashMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<T>,
        trash: Boolean
    )


    suspend fun <T: Media> deleteMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<T>
    )



    fun saveImage(
        originalUri: Uri,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ): Uri?

    fun overrideImage(
        originalUri: Uri,
        uri: Uri,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat
    ): Boolean

}
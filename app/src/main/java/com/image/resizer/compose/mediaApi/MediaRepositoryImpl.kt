/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityOptionsCompat
import com.dot.gallery.feature_node.data.data_source.mediastore.queries.AlbumsFlow
import com.image.resizer.compose.mediaApi.model.Album
import com.image.resizer.compose.mediaApi.model.ExifAttributes
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.model.Media.UriMedia
import com.image.resizer.compose.mediaApi.model.MediaOrder
import com.image.resizer.compose.mediaApi.model.MediaStoreBuckets
import com.image.resizer.compose.mediaApi.model.OrderType
import com.image.resizer.compose.mediaApi.model.Vault
import com.image.resizer.compose.mediaApi.util.getUri
import com.image.resizer.compose.mediaApi.util.mapAsResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MediaRepositoryImpl(
    val context: Context,
) : MediaRepository {

    private val contentResolver = context.contentResolver

    override fun getMedia(): Flow<Resource<List<UriMedia>>> =
        MediaFlow(
            contentResolver = contentResolver,
            buckedId = MediaStoreBuckets.MEDIA_STORE_BUCKET_TIMELINE.id
        ).flowData().map {
            Resource.Success(MediaOrder.Date(OrderType.Descending).sortMedia(it))
        }

    override fun getAlbums(mediaOrder: MediaOrder): Flow<Resource<List<Album>>> =
        AlbumsFlow(context).flowData().map {
            withContext(Dispatchers.IO) {
                val data = it.toMutableList().apply {
                    replaceAll { album ->
                        album.copy(isPinned = false)
                    }
                }

                Resource.Success(mediaOrder.sortAlbums(data))
            }
        }.flowOn(Dispatchers.IO)

    override fun getAlbumsWithType(allowedMedia: AllowedMedia): Flow<Resource<List<Album>>> =
        AlbumsFlow(
            context = context,
            mimeType = allowedMedia.toStringAny()
        ).flowData().mapAsResource()

    override fun getMediaByType(allowedMedia: AllowedMedia): Flow<Resource<List<UriMedia>>> =
        MediaFlow(
            contentResolver = contentResolver,
            buckedId = when (allowedMedia) {
                AllowedMedia.PHOTOS -> MediaStoreBuckets.MEDIA_STORE_BUCKET_PHOTOS.id
                AllowedMedia.VIDEOS -> MediaStoreBuckets.MEDIA_STORE_BUCKET_VIDEOS.id
                AllowedMedia.BOTH -> MediaStoreBuckets.MEDIA_STORE_BUCKET_TIMELINE.id
            },
            mimeType = allowedMedia.toStringAny()
        ).flowData().map {
            Resource.Success(it)
        }.flowOn(Dispatchers.IO)

    override fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<UriMedia>>> =
        MediaFlow(
            contentResolver = contentResolver,
            buckedId = albumId,
        ).flowData().mapAsResource()

    override suspend fun getCategoryForMediaId(mediaId: Long): String? {
        return null
    }

    override fun getMediaByAlbumIdWithType(
        albumId: Long,
        allowedMedia: AllowedMedia
    ): Flow<Resource<List<UriMedia>>> =
        MediaFlow(
            contentResolver = contentResolver,
            buckedId = albumId,
            mimeType = allowedMedia.toStringAny()
        ).flowData().mapAsResource()

    override fun getMediaListByUris(
        listOfUris: List<Uri>,
        reviewMode: Boolean
    ): Flow<Resource<List<UriMedia>>> =
        MediaUriFlow(
            contentResolver = contentResolver,
            uris = listOfUris,
            reviewMode = reviewMode
        ).flowData().mapAsResource(errorOnEmpty = true, errorMessage = "Media could not be opened")


    override suspend fun <T : Media> trashMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<T>,
        trash: Boolean
    ) {
        val intentSender = MediaStore.createTrashRequest(
            contentResolver,
            mediaList.map { it.getUri() },
            trash
        ).intentSender
        val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
            .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
            .build()
        result.launch(senderRequest, ActivityOptionsCompat.makeTaskLaunchBehind())
    }

    override fun getVaults(): Flow<Resource<List<Vault>>> = flow {
        Resource.Success(emptyList<Vault>())
    }

    override suspend fun <T : Media> deleteMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<T>
    ) {
        val intentSender =
            MediaStore.createDeleteRequest(
                contentResolver,
                mediaList.map { it.getUri() }).intentSender
        val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
            .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
            .build()
        result.launch(senderRequest)
    }

    override suspend fun <T : Media> updateMediaExif(
        media: T,
        exifAttributes: ExifAttributes
    ): Boolean = contentResolver.updateMediaExif(
        media = media,
        exifAttributes = exifAttributes
    )

    override fun saveImage(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ) = contentResolver.saveImage(bitmap, format, mimeType, relativePath, displayName)

    override fun overrideImage(
        uri: Uri,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ) = contentResolver.overrideImage(uri, bitmap, format)

}
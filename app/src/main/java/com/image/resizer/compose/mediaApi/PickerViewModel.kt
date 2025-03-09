/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.image.resizer.compose.mediaApi

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.image.resizer.compose.mediaApi.model.Album
import com.image.resizer.compose.mediaApi.model.AlbumState
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.model.MediaState
import com.image.resizer.compose.mediaApi.util.Constants
import com.image.resizer.compose.mediaApi.util.mapMedia
import com.image.resizer.compose.mediaApi.util.mediaFlowWithType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

open class PickerViewModel (
    private val repository: MediaRepository
) : ViewModel() {


    var allowedMedia: AllowedMedia = AllowedMedia.BOTH
    var albumId: Long = -1L
        set(value) {
            field = value
            mediaState = lazy {
                repository.mediaFlowWithType(value, allowedMedia).map{ mediaResult ->
                    val data = (mediaResult.data ?: emptyList()).toMutableList().apply {
                    }
                    val error = if (mediaResult is Resource.Error) mediaResult.message
                        ?: "An error occurred" else ""
                    if (error.isNotEmpty()) {
                        return@map Resource.Error<List<Media>>(message = error)
                    }
                    Resource.Success<List<Media>>(data)
                }.mapMedia(
                    albumId = value,
                    groupByMonth = false,
                    withMonthHeader = false,
                    updateDatabase = {},
                    defaultDateFormat = Constants.DEFAULT_DATE_FORMAT,
                    extendedDateFormat = Constants.EXTENDED_DATE_FORMAT,
                    weeklyDateFormat = Constants.WEEKLY_DATE_FORMAT
                ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), MediaState())
            }
        }

    var mediaState = lazy {
        repository.mediaFlowWithType(albumId, allowedMedia).map {  mediaResult ->
            val data = (mediaResult.data ?: emptyList()).toMutableList().apply {
            }
            val error = if (mediaResult is Resource.Error) mediaResult.message
                ?: "An error occurred" else ""
            if (error.isNotEmpty()) {
                return@map Resource.Error<List<Media>>(message = error)
            }
            Resource.Success<List<Media>>(data)
        }.mapMedia(
            albumId = albumId,
            groupByMonth = false,
            withMonthHeader = false,
            updateDatabase = {},
            defaultDateFormat = Constants.DEFAULT_DATE_FORMAT,
            extendedDateFormat = Constants.EXTENDED_DATE_FORMAT,
            weeklyDateFormat = Constants.WEEKLY_DATE_FORMAT
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), MediaState())
    }

    val albumsState by lazy {
        repository.getAlbumsWithType(allowedMedia).map {  albumsResult ->
            val data = (albumsResult.data ?: emptyList()).toMutableList().apply {
            }
            val error = if (albumsResult is Resource.Error) albumsResult.message
                ?: "An error occurred" else ""
            if (data.isEmpty()) {
                return@map AlbumState(albums = listOf(emptyAlbum), error = error)
            }
            val albums = mutableListOf<Album>().apply {
                add(emptyAlbum)
                addAll(data)
            }
            AlbumState(albums = albums, error = error)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AlbumState())
    }


    private val emptyAlbum = Album(
        id = -1,
        label = "All",
        uri = Uri.EMPTY,
        pathToThumbnail = "",
        timestamp = 0,
        relativePath = ""
    )

}

enum class AllowedMedia {
    PHOTOS, VIDEOS, BOTH;

    override fun toString(): String {
        return when (this) {
            PHOTOS -> "image%"
            VIDEOS -> "video%"
            BOTH -> "%/%"
        }
    }

    fun toStringAny(): String {
        return when (this) {
            PHOTOS -> "image/*"
            VIDEOS -> "video/*"
            BOTH -> "*/*"
        }
    }
}
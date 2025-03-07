package com.image.resizer.compose.mediaApi

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.image.resizer.compose.Screen
import com.image.resizer.compose.mediaApi.model.Album
import com.image.resizer.compose.mediaApi.model.AlbumState
import com.image.resizer.compose.mediaApi.model.Media.UriMedia
import com.image.resizer.compose.mediaApi.model.MediaOrder
import com.image.resizer.compose.mediaApi.model.OrderType
import com.image.resizer.compose.mediaApi.util.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.get

class AlbumsViewModel constructor(
    private val repository: MediaRepository,val handleUseCase: MediaHandleUseCase
) : MediaViewModel(repository,handleUseCase) {
    private val albumOrder: MediaOrder
        get() = MediaOrder.Date(OrderType.Descending)
    val multiSelectState = mutableStateOf(false)
    val selectedPhotoState = mutableStateListOf<UriMedia>()

    fun toggleSelection(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = mediaFlow.value.media[index]
            val selectedPhoto = selectedPhotoState.find { it.id == item.id }
            if (selectedPhoto != null) {
                selectedPhotoState.remove(selectedPhoto)
            } else {
                selectedPhotoState.add(item)
            }
            multiSelectState.update(selectedPhotoState.isNotEmpty())
        }
    }
    val albumsFlow = repository.getAlbums(mediaOrder = albumOrder).map { result ->
        val newOrder = albumOrder
        val data = newOrder.sortAlbums(result.data ?: emptyList())
        val cleanData = data

        AlbumState(
            albums = cleanData,
            albumsWithBlacklisted = data,
            albumsUnpinned = cleanData.filter { !it.isPinned },
            albumsPinned = cleanData.filter { it.isPinned }.sortedBy { it.label },
            isLoading = false,
            error = if (result is Resource.Error) result.message ?: "An error occurred" else ""
        )
    }.stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(), AlbumState())

    fun onAlbumClick(navigate: (String) -> Unit): (Album) -> Unit = { album ->
        navigate(Screen.AlbumViewScreen.route + "?albumId=${album.id}&albumName=${album.label}")
    }
}
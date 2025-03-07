package com.image.resizer.compose.mediaApi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.model.MediaState
import com.image.resizer.compose.mediaApi.model.Vault
import com.image.resizer.compose.mediaApi.model.VaultState
import com.image.resizer.compose.mediaApi.util.Constants
import com.image.resizer.compose.mediaApi.util.mapMediaToItem
import com.image.resizer.compose.mediaApi.util.mediaFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

open class MediaViewModel constructor(
    private val repository: MediaRepository,
    val handler: MediaHandleUseCase
) : ViewModel() {
    var albumId: Long = -1L
    var target: String? = null
    var category: String? = null
    private val permissionState = MutableStateFlow(false)


    val mediaFlow by lazy {
        combine(
            repository.mediaFlow(albumId, target),
            permissionState
        ) { result, hasPermission ->
            if (result is Resource.Error) return@combine MediaState(
                error = result.message ?: "",
                isLoading = false
            )
            mapMediaToItem(
                data = (result.data ?: emptyList()).toMutableList().apply {
                },
                error = result.message ?: "",
                albumId = albumId,
                groupByMonth =  true,
                defaultDateFormat = Constants.DEFAULT_DATE_FORMAT,
                extendedDateFormat = Constants.EXTENDED_DATE_FORMAT,
                weeklyDateFormat = Constants.WEEKLY_DATE_FORMAT
            )
        }.stateIn(viewModelScope, started = SharingStarted.Eagerly, MediaState())
    }

    val vaultsFlow = repository.getVaults()
        .map { it.data ?: emptyList() }
        .map { VaultState(it, isLoading = false) }
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, VaultState())

    fun <T : Media> addMedia(vault: Vault, media: T) {
        viewModelScope.launch(Dispatchers.IO) {
           // repository.addMedia(vault, media)
        }
    }

}
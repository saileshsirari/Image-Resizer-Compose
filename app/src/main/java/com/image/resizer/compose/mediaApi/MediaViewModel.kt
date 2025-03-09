package com.image.resizer.compose.mediaApi

import android.R.attr.bitmap
import android.content.Context
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Environment
import android.util.Log.e
import androidx.annotation.Keep
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.image.resizer.compose.ImageItem
import com.image.resizer.compose.ImageReplacer
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.model.MediaState
import com.image.resizer.compose.mediaApi.model.Vault
import com.image.resizer.compose.mediaApi.model.VaultState
import com.image.resizer.compose.mediaApi.util.Constants
import com.image.resizer.compose.mediaApi.util.mapMediaToItem
import com.image.resizer.compose.mediaApi.util.mediaFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Keep
sealed interface SaveFormat {

    val format: CompressFormat
    val mimeType: String

    data object PNG : SaveFormat {
        override val format = CompressFormat.PNG
        override val mimeType = "image/png"
    }
}

open class MediaViewModel(
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
                groupByMonth = true,
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

    private val _isSaving = MutableStateFlow(true)
    val isSaving = _isSaving.asStateFlow()
    fun saveOverride(
        context: Context,
        listImageItems: List<ImageItem>,
        saveFormat: SaveFormat = SaveFormat.PNG,
        onSuccess: () -> Unit = {},
        onFail: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            delay(500)
            val mediaList =
                repository.getMediaListByUris(listImageItems.map { it.uri }, reviewMode = false)
                    .firstOrNull()?.data
                    ?: emptyList()
            var replaced = true
            listImageItems.forEach {
                it.let { imageItem ->
                    try {

                        imageItem.scaledBitmap?.let {
                            replaced = ImageReplacer.replaceOriginalImageWithBitmap(
                                context,
                                imageItem.uri,
                                it
                            )
                        }
                        /*  if (handler.overrideImage(
                                  uri = imageItem.uri,
                                  bitmap = it,
                                  format = saveFormat.format,
                                  relativePath = Environment.DIRECTORY_PICTURES + "/Edited",
                                  displayName = imageItem.imageName
                                      ?: ("${System.currentTimeMillis()} ok"),
                                  mimeType = saveFormat.mimeType
                              )
                              onSuccess().also { _isSaving.value = false }
                          } else {
                              onFail().also { _isSaving.value = false }
                          }


                      }
                         */
                        onSuccess()
                    } catch (e: Exception) {
                        onFail().also { _isSaving.value = false }
                    }
                } ?: onFail().also { _isSaving.value = false }
            }
        }
    }

}
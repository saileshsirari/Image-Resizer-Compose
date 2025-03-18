package com.image.resizer.compose

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.image.resizer.compose.ImageHelper.getFileNameAndSize
import com.image.resizer.compose.mediaApi.MediaHandleUseCase
import com.image.resizer.compose.mediaApi.SaveFormat
import com.image.resizer.compose.mediaApi.loadBitmapFromUri
import com.image.resizer.compose.mediaApi.model.Album
import com.image.resizer.compose.mediaApi.util.Constants.CUSTOM_FOLDER_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.format

class HomeScreenViewModel(
    private val mediaHandler: MediaHandleUseCase
) : ViewModel() {
    private val _showToast = MutableStateFlow("")
    val showToast: StateFlow<String> = _showToast
    private val _cropState = MutableStateFlow<CropState>(CropState.Idle)
    val cropState: StateFlow<CropState> = _cropState

    // val selectedUris: Flow<List<Uri>> = selectedMediaRepository.getSelectedMedia()
    private val _compressState = MutableStateFlow<CompressState>(CompressState.Idle)
    val compressState: StateFlow<CompressState> = _compressState
    private val _isSaving = MutableStateFlow(true)
    val isSaving = _isSaving.asStateFlow()
    private val _scaleState = MutableStateFlow<ScaleState>(ScaleState.Idle)
    val scaleState: StateFlow<ScaleState> = _scaleState

    private val _galleryState = MutableStateFlow<GalleryState>(GalleryState.Idle)
    val galleryState: StateFlow<GalleryState> = _galleryState
    var selectedItem: ImageItem? = null

    var selectedImageItems: List<ImageItem> = emptyList()

    fun onCropSuccess(croppedUri: Uri?) {
        viewModelScope.launch {
            onReset()
            _cropState.value = CropState.Success(CropStateData(croppedUri))
        }
    }

    fun showToast(value: String = "Image saved") {
        _showToast.value = value
    }

    fun onCropScreenLaunched() {
        viewModelScope.launch {
            _cropState.value = CropState.Idle
        }
    }


    fun onCropError(message: String) {
        viewModelScope.launch {
            _cropState.value = CropState.Error(message)
        }
    }

    fun onShowCompressPopup() {
        viewModelScope.launch {
            onReset()
            _compressState.value = CompressState.PopupShown
        }
    }


    fun onShowScalePopup() {
        onReset()
        _scaleState.value = ScaleState.ShowPopup
    }

    fun onImagesScaled(scaleParamsList: List<ScaleParams>) {
        onReset()
        _scaleState.value = ScaleState.Success(ScaleStateData(scaleParamsList))
    }

    fun handlePickedImages(
        uris: List<@JvmSuppressWildcards Uri>,
        context: Context,
        callBack: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uris.isNotEmpty()) {
                callBack()
                val selectedImageItems = uris.map { uri ->
                //    val (imageName, fileSize) = getFileNameAndSize(context, uri)
                    ImageItem(
                        context = context,
                        uri = uri,
                    )
                }
                onGalleryImagesSelected(selectedImageItems)
            }
        }
    }

    fun onGalleryImagesSelected(imageItems: List<ImageItem>) {
        viewModelScope.launch {
            onReset()
            selectedImageItems = imageItems
            _galleryState.value = GalleryState.Loading
            _galleryState.value = GalleryState.Success(GalleryStateData(imageItems))
        }
    }

    fun onUndo() {
        _cropState.value = CropState.Idle
        _compressState.value = CompressState.Idle
        _scaleState.value = ScaleState.Idle
        _galleryState.value = GalleryState.Success(GalleryStateData(selectedImageItems))
    }

    fun showSelectedImages() {
        _cropState.value = CropState.Idle
        _compressState.value = CompressState.Idle
        _scaleState.value = ScaleState.Idle
        _galleryState.value = GalleryState.Success(GalleryStateData(selectedImageItems))
    }

    fun onCompressImagesSaved() {
        viewModelScope.launch {
            _compressState.value = CompressState.ImagesSaved
        }
    }

    fun onReset() {
        _cropState.value = CropState.Idle
        _compressState.value = CompressState.Idle
        _scaleState.value = ScaleState.Idle
        _galleryState.value = GalleryState.Idle
    }

    fun onCompressCancel() {
        viewModelScope.launch {
            onReset()
            _compressState.value = CompressState.Idle
            _galleryState.value = GalleryState.Success(GalleryStateData(selectedImageItems))
        }
    }

    fun onCompressShowImages(size: Int) {
        viewModelScope.launch {
            onReset()
            _compressState.value = CompressState.Success(CompressStateData(size))
        }
    }

    fun onShowCropPopup() {
        _cropState.value = CropState.PopupShown
    }

    fun dismissScalePopup() {
        _scaleState.value = ScaleState.Idle
    }

    fun saveImagesToGallery(
        imageItems: List<ImageItem?>,
        customDirectoryName: String = "ImageResizer"
    ) {
        _isSaving.value = true


    }

    fun saveCopy(
        saveFormat: SaveFormat=SaveFormat.JPEG ,
        onSuccess: () -> Unit = {},
        onFail: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            viewModelScope.launch(Dispatchers.IO) {
                _isSaving.value = true
                delay(500)
                selectedImageItems.forEach {
                    val media = it
                    val currentBitmap = it.scaledBitmap
                    currentBitmap?.let { bitmap ->
                        try {
                            val displayName =
                                media.imageName?:"imageResizer_${
                                    SimpleDateFormat(
                                        "MM_dd_HH_mm_ss",
                                        Locale.getDefault()
                                    ).format(Date())
                                }.jpg"
                            if (mediaHandler.saveImage(
                                    bitmap = bitmap,
                                    format = saveFormat.format,
                                    relativePath = Environment.DIRECTORY_PICTURES + "/" + CUSTOM_FOLDER_NAME,
                                    displayName = media.imageName?:displayName,
                                    mimeType = saveFormat.mimeType
                                ) != null
                            ) {
                                onSuccess().also { _isSaving.value = false }
                            } else {
                                onFail().also { _isSaving.value = false }
                            }
                        } catch (_: Exception) {
                            _isSaving.value = false
                            onFail().also { _isSaving.value = false }
                        }
                    } ?: onFail().also { _isSaving.value = false }
                }
            }
        }
    }

    fun saveOverride(
        saveFormat: SaveFormat= SaveFormat.JPEG,
        onSuccess: () -> Unit = {},
        onFail: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            delay(500)
            selectedImageItems.forEach {
                val media = it
                val currentBitmap = it.scaledBitmap
                currentBitmap?.let { bitmap ->
                    try {
                        if (mediaHandler.overrideImage(
                                uri = media.uri,
                                bitmap = bitmap,
                                format = saveFormat.format
                            )
                        ) {
                            onSuccess().also { _isSaving.value = false }
                        } else {
                            onFail().also { _isSaving.value = false }
                        }
                    } catch (e: Exception) {
                        onFail().also { _isSaving.value = false }
                    }
                } ?: onFail().also { _isSaving.value = false }
            }
        }
    }


    fun onSelectedItemClicked(item: ImageItem,navigate: (String) -> Unit) {
        selectedItem = item
        navigate(Screen.ImageDetailScreen.route )
    }

}
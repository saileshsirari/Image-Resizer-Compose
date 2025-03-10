package com.image.resizer.compose

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.image.resizer.compose.ImageHelper.getFileNameAndSize
import com.image.resizer.compose.mediaApi.MediaHandleUseCase
import com.image.resizer.compose.mediaApi.SaveFormat
import com.image.resizer.compose.mediaApi.SelectedMediaRepository
import com.image.resizer.compose.mediaApi.model.Album
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel(private val selectedMediaRepository: SelectedMediaRepository,
                          private val mediaHandler: MediaHandleUseCase) : ViewModel() {
    private  val _showToast = MutableStateFlow("")
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

    var selectedImageItems: List<ImageItem> = emptyList()

    fun onCropSuccess(croppedUri: Uri?) {
        viewModelScope.launch {
            onReset()
            _cropState.value = CropState.Success(CropStateData(croppedUri))
        }
    }
    fun showToast(value: String="Image saved"){
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
        callBack:()->Unit
    ) {
        if (uris.isNotEmpty()) {
            val selectedImageItems = uris.map { uri ->
                val (imageName, fileSize) = getFileNameAndSize(context, uri)
                val imagesDimensions = imageDimensionsFromUri(context, uri)
                ImageItem(
                    uri,
                    imageName = imageName,
                    fileSize = fileSize,
                    imageDimension = imagesDimensions
                )
            }
            onGalleryImagesSelected(selectedImageItems)
            callBack()
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

    fun onCompressShowImages(size:Int) {
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

    fun onFabClicked(navigate: (String) -> Unit): () -> Unit = {
        navigate(Screen.AlbumsScreen.route )
    }

     fun clearSelectedUri() {
         viewModelScope.launch {
             selectedMediaRepository.clearSelectedMedia()
         }
    }

    fun saveOverride(
        saveFormat: SaveFormat = SaveFormat.PNG,
        onSuccess: () -> Unit = {},
        onFail: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            delay(500)
            val media = selectedImageItems.first()
            val currentBitmap = selectedImageItems.first().scaledBitmap
            currentBitmap?.let { bitmap ->
                try {
                    if (mediaHandler.overrideImage(
                            uri = media.uri,
                            bitmap = bitmap,
                            format = saveFormat.format,
                            relativePath = Environment.DIRECTORY_PICTURES + "/Edited",
                            displayName = media.imageName?:"test",
                            mimeType = saveFormat.mimeType
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
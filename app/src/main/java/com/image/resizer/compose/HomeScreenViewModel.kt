package com.image.resizer.compose

import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel() : ViewModel() {
    private val _cropState = MutableStateFlow<CropState>(CropState.Idle)
    val cropState: StateFlow<CropState> = _cropState

    private val _compressState = MutableStateFlow<CompressState>(CompressState.Idle)
    val compressState: StateFlow<CompressState> = _compressState

    private val _scaleState = MutableStateFlow<ScaleState>(ScaleState.Idle)
    val scaleState: StateFlow<ScaleState> = _scaleState

    private val _galleryState = MutableStateFlow<GalleryState>(GalleryState.Idle)
    val galleryState: StateFlow<GalleryState> = _galleryState

    var selectedImageUris: List<Uri> = emptyList()

    fun onCropSuccess(croppedUri: Uri) {
        viewModelScope.launch {
            _cropState.value = CropState.Success(CropStateData(croppedUri))
        }
    }

    fun onCropError(message: String) {
        viewModelScope.launch {
            _cropState.value = CropState.Error(message)
        }
    }

    fun onShowCompressPopup() {
        viewModelScope.launch {
            _compressState.value = CompressState.PopupShown
        }
    }

    fun onCompressDone(list: List<Uri>) {
        viewModelScope.launch {
            _compressState.value = CompressState.Success(CompressStateData(list))
        }
    }

    fun onScalePopup(scaleParamsList: List<ScaleParams>) {
        viewModelScope.launch {
            _scaleState.value = ScaleState.Loading
            _scaleState.value = ScaleState.Success(ScaleStateData(scaleParamsList))
        }
    }

    fun onGalleryImagesSelected(imageUris: List<Uri>) {
        viewModelScope.launch {
            onReset()
            selectedImageUris = imageUris
            _galleryState.value = GalleryState.Loading
            _galleryState.value = GalleryState.Success(GalleryStateData(imageUris))
        }
    }

    fun onUndo() {
        _cropState.value = CropState.Idle
        _compressState.value = CompressState.Idle
        _scaleState.value = ScaleState.Idle
        _galleryState.value = GalleryState.Success(GalleryStateData(selectedImageUris))
    }

    fun showSelectedImages() {
        _cropState.value = CropState.Idle
        _compressState.value = CompressState.Idle
        _scaleState.value = ScaleState.Idle
        _galleryState.value = GalleryState.Success(GalleryStateData(selectedImageUris))
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
            _galleryState.value = GalleryState.Success(GalleryStateData(selectedImageUris))
        }
    }

    fun onCompressShowImages() {
        viewModelScope.launch {
            onReset()
            _compressState.value = CompressState.ImagesShown
        }
    }

}
package com.image.resizer.compose

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log.e
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.image.resizer.compose.mediaApi.MediaHandleUseCase
import com.image.resizer.compose.mediaApi.SaveFormat
import com.image.resizer.compose.mediaApi.loadBitmapFromUri
import com.image.resizer.compose.mediaApi.util.Constants.CUSTOM_FOLDER_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private val _scaledImageItems = MutableStateFlow<List<ImageItem>>(emptyList())
    val scaledImageItems = _scaledImageItems.asStateFlow()
    var _selectedImageItems = MutableStateFlow<List<ImageItem>>(emptyList())

    var selectedImageItems = _selectedImageItems.asStateFlow()

    fun compressImagesToTargetSize(
        context: Context,
        imageItems: List<ImageItem>,
        percentOriginal: Int = TARGET_FILE_SIZE_KB
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _scaledImageItems.value = emptyList<ImageItem>()
            ImageScalar().compressImagesToTargetSize(context, imageItems.take(25), percentOriginal)
                .collect {
                    _scaledImageItems.value = _scaledImageItems.value + it
                }
        }
    }

    fun scaleImages(
        imageItems: List<ImageItem>,
        scaleParamsList: List<ScaleParams>,
        context: Context,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _scaledImageItems.value = emptyList<ImageItem>()
            ImageScalar().scaleImages(
                context = context,
                imageItems = imageItems,
                scaleParamsList = scaleParamsList
            )
                .collect {
                    _scaledImageItems.value = _scaledImageItems.value + it
                }
        }

    }

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

    fun onImagesScaled(context: Context,scaleParamsList: List<ScaleParams>) {
        onReset()
        scaleImages(selectedImageItems.value, scaleParamsList, context)
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
        viewModelScope.launch(Dispatchers.IO) {
            onReset()
            _galleryState.value = GalleryState.Loading

            _galleryState.value = GalleryState.Success(GalleryStateData(emptyList()))

            // list.addAll(chunkedItems)
            _selectedImageItems.value = imageItems
        }
    }

    fun onUndo() {
        _cropState.value = CropState.Idle
        _compressState.value = CompressState.Idle
        _scaleState.value = ScaleState.Idle
        _galleryState.value = GalleryState.Success(GalleryStateData(_selectedImageItems.value))
    }

    fun showSelectedImages() {
        _cropState.value = CropState.Idle
        _compressState.value = CompressState.Idle
        _scaleState.value = ScaleState.Idle
        _galleryState.value = GalleryState.Success(GalleryStateData(_selectedImageItems.value))
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
            _galleryState.value = GalleryState.Success(GalleryStateData(_selectedImageItems.value))
        }
    }

    fun onCompressShowImages(context:Context , size: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            onReset()
            compressImagesToTargetSize(context,selectedImageItems.value,size)
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
        context: Context,
        saveFormat: SaveFormat = SaveFormat.JPEG,
        onSuccess: () -> Unit = {},
        onFail: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            val currentSelectedItems = _scaledImageItems.value
            currentSelectedItems.forEach {
                val media = it
                try {
                    it.scaledUri?.let { scaledUri ->
                        val currentBitmap = loadBitmapFromUri(scaledUri, context)
                        currentBitmap?.let { bitmap ->

                            val displayName =
                                media.imageName ?: "imageResizer_${
                                    SimpleDateFormat(
                                        "MM_dd_HH_mm_ss",
                                        Locale.getDefault()
                                    ).format(Date())
                                }.jpg"
                            if (mediaHandler.saveImage(
                                    bitmap = bitmap,
                                    format = saveFormat.format,
                                    relativePath = Environment.DIRECTORY_PICTURES + "/" + CUSTOM_FOLDER_NAME,
                                    displayName = media.imageName ?: displayName,
                                    mimeType = saveFormat.mimeType
                                ) == null
                            ) {
                                throw Exception("Unable to save")
                            }
                        }
                    }
                } catch (_: Exception) {
                    _isSaving.value = false
                    onFail().also { _isSaving.value = false }

                } ?: onFail().also { _isSaving.value = false }
                onSuccess().also { _isSaving.value = false }
            }
        }
    }

    fun saveOverride(
        context: Context,
        saveFormat: SaveFormat = SaveFormat.JPEG,
        onSuccess: () -> Unit = {},
        onFail: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            _isSaving.value = true
            try {
                val currentSelectedItems = _scaledImageItems.value
                currentSelectedItems.forEach {
                    val media = it
                    it.scaledUri?.let { scaledUri ->
                        val currentBitmap = loadBitmapFromUri(scaledUri, context)
                        currentBitmap?.let { bitmap ->
                            if (!mediaHandler.overrideImage(
                                    uri = media.uri,
                                    bitmap = bitmap,
                                    format = saveFormat.format
                                )
                            ) {
                                throw Exception("Unable to save")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                onFail().also { _isSaving.value = false }
            }
            onSuccess().also { _isSaving.value = false }
        }
    }


    fun onSelectedItemClicked(item: ImageItem, navigate: (String) -> Unit) {
        selectedItem = item
        navigate(Screen.ImageDetailScreen.route)
    }

}
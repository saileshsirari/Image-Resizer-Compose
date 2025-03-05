package com.image.resizer.compose

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyImagesViewModel(private val imageRepository: ImageRepository) : ViewModel() {
    private val _loadingImages = MutableStateFlow<Set<Int>>(emptySet())
    val loadingImages: StateFlow<Set<Int>> = _loadingImages.asStateFlow()

    private val _actualImageItems = MutableStateFlow<List<ImageItem?>>(emptyList())
    val actualImageUris: StateFlow<List<ImageItem?>> = _actualImageItems.asStateFlow()

    private val _selectedImages = MutableStateFlow<List<ImageItem>>(emptyList())
    val selectedImages: StateFlow<List<ImageItem>> = _selectedImages.asStateFlow()

    private val _selectAll = MutableStateFlow(false)
    val selectAll: StateFlow<Boolean> = _selectAll.asStateFlow()

    private val _imageSelectionMode = MutableStateFlow(false)
    val imageSelectionMode: StateFlow<Boolean> = _imageSelectionMode.asStateFlow()

    private val _showShareSheet = MutableStateFlow(false)
    val showShareSheet: StateFlow<Boolean> = _showShareSheet.asStateFlow()
    private val _showDeleteConfirmationDialog = MutableStateFlow(false)
    val showDeleteConfirmationDialog: StateFlow<Boolean> =
        _showDeleteConfirmationDialog.asStateFlow()

    private val _totalImagesCount =
        MutableStateFlow(imageRepository.getTotalCompressedImagesCount())
    val totalImagesCount: StateFlow<Int> = _totalImagesCount.asStateFlow()

    init {
        loadImages()
    }

    fun loadImages() {
        viewModelScope.launch {
            val totalImagesCount = imageRepository.getTotalCompressedImagesCount()
            val newImages = mutableSetOf<Int>()
            for (i in 1..totalImagesCount) {
                newImages.add(i)
            }
            _loadingImages.value = newImages
            val urisForPage = withContext(Dispatchers.IO) {
                imageRepository.getRealCompressedImageUris(newImages.toList())
            }
            _actualImageItems.value = urisForPage
        }
    }

    fun deleteImages(images: List<ImageItem>) {
        viewModelScope.launch {
            val list = _actualImageItems.value.toMutableList()
            images.forEach {
                list.remove(it)
            }
            _actualImageItems.value = list
            imageRepository.deleteImages(images)
            clearSelection()
        }
    }

    fun onImageSelected(imageItem: ImageItem) {
        if (_selectedImages.value.contains(imageItem)) {
            val newList = _selectedImages.value.filter { it != imageItem }
            _selectedImages.value = newList
        } else {
            _selectedImages.value = _selectedImages.value + imageItem
        }

    }

    fun clearSelection() {
        _selectedImages.value = emptyList()
        _selectAll.value = false
        _imageSelectionMode.value = false
    }

    fun selectAllImages() {
        _selectAll.value = !_selectAll.value
        _imageSelectionMode.value = true
        if (_selectAll.value) {
            _selectedImages.value = _actualImageItems.value.filterNotNull()
        } else {
            clearSelection()
        }
    }

    fun setShowDeleteConfirmationDialog(show: Boolean) {
        _showDeleteConfirmationDialog.value = show
    }

    fun setShowShareSheet(show: Boolean) {
        _showShareSheet.value = show
    }

}
package com.image.resizer.compose

import android.net.Uri

// Common interface for all state data
sealed interface OperationStateData

// Common states
sealed class OperationState {
    data object Idle : OperationState()
    data object Loading : OperationState()
    data class Success(val data: OperationStateData) : OperationState()
    data class Error(val message: String) : OperationState()
}

// Crop
data class CropStateData(val croppedImageUri: Uri) : OperationStateData
sealed class CropState : OperationState() {
    data object Idle : CropState()
    data object Loading : CropState()
    data class Success(val data: CropStateData) : CropState()
    data class Error(val message: String) : CropState()
}

// Compress
data class CompressStateData(val size:Int) : OperationStateData
sealed class CompressState : OperationState() {
    data object Idle : CompressState()
    data object PopupShown : CompressState()
    data object ImagesSaved : CompressState()
    data class Success(val data: CompressStateData) : CompressState()
    data class Error(val message: String) : CompressState()
}

// Scale
data class ScaleStateData(val scaleParamsList: List<ScaleParams>) : OperationStateData
sealed class ScaleState : OperationState() {
    data object Idle : ScaleState()
    data object Loading : ScaleState()
    data object ShowPopup : ScaleState()
    data class Success(val data: ScaleStateData) : ScaleState()
    data class Error(val message: String) : ScaleState()
}

// Gallery
data class GalleryStateData(val imageUris: List<Uri>) : OperationStateData
sealed class GalleryState : OperationState() {
    data object Idle : GalleryState()
    data object Loading : GalleryState()
    data class Success(val data: GalleryStateData) : GalleryState()
    data class Error(val message: String) : GalleryState()
}
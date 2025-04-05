package com.image.resizer.compose

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.util.Log.e
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.image.resizer.compose.mediaApi.MediaHandleUseCase
import com.image.resizer.compose.mediaApi.SaveFormat
import com.image.resizer.compose.mediaApi.clearCache
import com.image.resizer.compose.mediaApi.loadBitmapFromUri
import com.image.resizer.compose.mediaApi.pruneInternalStorage
import com.image.resizer.compose.mediaApi.util.Constants.CUSTOM_FOLDER_NAME
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield


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
    private var _selectedImageItems = MutableStateFlow<List<ImageItem>>(emptyList())
    var selectedImageItems = _selectedImageItems.asStateFlow()
    private var _savingState = MutableStateFlow<Int>(0)
    var savingState = _savingState.asStateFlow()

    init {
        log("HomeScreenViewModel init")
    }

    fun compressImagesToTargetSize(
        context: Context,
        imageItems: List<ImageItem>,
        percentOriginal: Int = TARGET_PERCENTAGE
    ) {
        _scaledImageItems.value = imageItems
        ImageItem.percentScale = percentOriginal
        processSomeImages(imageItems)
        /* compress(context, imageItems, percentOriginal)
             .collect {
                 _scaledImageItems.value = _scaledImageItems.value + it
             }*/
    }

    private fun processSomeImages(
        imageItems: List<ImageItem>,
        operation: String = "compress",
        size: Int = 50
    ) {
        imageItems.take(size).forEachIndexed { index, it ->
            viewModelScope.launch(Dispatchers.Default) {
                println("$operation $index " + it.scaledUri.toString().take(10))
            }
        }
    }

    fun scaleImages(
        imageItems: List<ImageItem>,
        scaleParams: ScaleParams?,
    ) {
        _scaledImageItems.value = emptyList<ImageItem>()
        ImageItem.scaleParams = scaleParams
        _scaledImageItems.value = imageItems
        processSomeImages(imageItems, "scaleImages")
    }

    fun onCropSuccess(context: Context, croppedUri: Uri?) {
        viewModelScope.launch {
            onReset(context)
            _scaledImageItems.value = listOf(
                ImageItem(
                    context = context, uri = _selectedImageItems.value.first().uri,
                    computedUri = croppedUri
                )
            )
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

    fun onShowCompressPopup(context: Context) {
        viewModelScope.launch {
            onReset(context)
            _compressState.value = CompressState.PopupShown
        }
    }


    fun onShowScalePopup(context: Context) {
        onReset(context)
        _scaleState.value = ScaleState.ShowPopup
    }

    fun onImagesScaled(context: Context, scaleParams: ScaleParams?) {
        log("onImagesScaled")
        viewModelScope.launch(Dispatchers.IO) {
            onReset(context)
            scaleImages(selectedImageItems.value, scaleParams)
            _scaleState.value = ScaleState.Success(ScaleStateData(scaleParams))

        }
    }

    fun onCompressShowImages(context: Context, size: Int) {
        log("onCompressShowImages")
        viewModelScope.launch(Dispatchers.IO) {
            onReset(context)
            compressImagesToTargetSize(context, selectedImageItems.value, size)
            _compressState.value = CompressState.Success(CompressStateData(size))

        }
    }

    fun handlePickedImages(
        imageItems: List<@JvmSuppressWildcards ImageItem>,
        context: Context,
        callBack: () -> Unit
    ) {
        log("handlePickedImages")
        viewModelScope.launch(Dispatchers.IO) {
            if (imageItems.isNotEmpty()) {
                callBack()
                clearCache(context)
                pruneInternalStorage(context)
                onGalleryImagesSelected(imageItems)
            }
        }
    }

    fun onGalleryImagesSelected(imageItems: List<ImageItem>) {
        log("onGalleryImagesSelected")
        _cropState.value = CropState.Idle
        _compressState.value = CompressState.Idle
        _scaleState.value = ScaleState.Idle
        _galleryState.value = GalleryState.Loading

        _galleryState.value = GalleryState.Success(GalleryStateData(emptyList()))


        // list.addAll(chunkedItems)
        _selectedImageItems.value = imageItems

    }

    fun onUndo(context: Context) {
        log("onUndo")
        _cropState.value = CropState.Idle
        _compressState.value = CompressState.Idle
        _scaleState.value = ScaleState.Idle
        _scaledImageItems.value = emptyList<ImageItem>()
        val selectedImageItems = _selectedImageItems.value.map { it ->
            //    val (imageName, fileSize) = getFileNameAndSize(context, uri)
            ImageItem(
                context = context,
                uri = it.uri,
                imageName = it.imageName,
                size = it.size
            )
        }
        _selectedImageItems.value = selectedImageItems
        _galleryState.value = GalleryState.Success(GalleryStateData(_selectedImageItems.value))
        clearCache(context)
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

    fun onReset(context: Context) {
        log("onReset")
        _cropState.value = CropState.Idle
        _compressState.value = CompressState.Idle
        _scaleState.value = ScaleState.Idle
        _galleryState.value = GalleryState.Idle
        _scaledImageItems.value = emptyList<ImageItem>()
        val selectedImageItems = _selectedImageItems.value.map { it ->
            //    val (imageName, fileSize) = getFileNameAndSize(context, uri)
            ImageItem(
                context = context,
                uri = it.uri,
                imageName = it.imageName,
                size = it.size
            )
        }
        _selectedImageItems.value = selectedImageItems
        //onGalleryImagesSelected(context,selectedImageItems)
        ImageItem.percentScale = null
        ImageItem.scaleParams = null
    }

    fun onCompressCancel(context: Context) {
        viewModelScope.launch {
            onReset(context)
            _compressState.value = CompressState.Idle
            _galleryState.value = GalleryState.Success(GalleryStateData(_selectedImageItems.value))
        }
    }


    fun onShowCropPopup(context: Context) {
        _cropState.value = CropState.PopupShown
    }

    fun dismissScalePopup() {
        _scaleState.value = ScaleState.Idle
        _galleryState.value = GalleryState.Success(GalleryStateData(_selectedImageItems.value))
    }

    fun saveImagesToGallery(
        imageItems: List<ImageItem?>,
        customDirectoryName: String = "ImageResizer"
    ) {
        _isSaving.value = true
    }

    private fun isEnoughSpaceAvailable(numberOfImages: Int): Boolean {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val megabytesAvailable = bytesAvailable / (1024 * 1024)

        //Estimation of memory required.
        val bytesRequired = numberOfImages * 1 * 1024 * 1024 //1Mb by Image.

        if (bytesAvailable < bytesRequired) {
            println("Not enough storage space: $megabytesAvailable mb available")
            return false
        }
        println("Enough storage space : $megabytesAvailable mb available")
        return true
    }

    var job: Job? = null

    fun cancelSave() {
        log("cancelSave")
        job?.cancel()
        _isSaving.value = false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun saveCopy(
        context: Context,
        saveFormat: SaveFormat = SaveFormat.JPEG,
        onSuccess: (String) -> Unit = {},
        onFail: (String) -> Unit = {}
    ) {
        log("saveCopy")
        val exceptionHandler = CoroutineExceptionHandler { _, e ->
            println("[ERROR] ${e.message}")
        }
        val mutex = Mutex()
        _isSaving.value = true
        val currentSelectedItems = _scaledImageItems.value
        val processed = mutableListOf<ImageItem>()
        var count = 0
        //Check if the space is enough
        if (!isEnoughSpaceAvailable(currentSelectedItems.size)) {
            _isSaving.value = false
            onFail("Not enough space available").also { _isSaving.value = false }
            println("Not enough space available")
            return
        }
        _savingState.value = 2
        job = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {

                currentSelectedItems.asFlow().flowOn(Dispatchers.IO).chunked(10).map { list ->
                    println("wait for this list $count")
                    val defList = mutableListOf<Deferred<Any?>>()
                    list.mapIndexed { index, it ->
                        val def = async(exceptionHandler + Dispatchers.IO) {
                            yield()
                            //   mutex.withLock {
                            try {
                                //   val saveJobs =   currentSelectedItems.mapIndexed { index, it ->
                                val media = it
                                it.scaledUri?.let { scaledUri ->
                                    var bitmap: Bitmap? = null

                                    bitmap = loadBitmapFromUri(scaledUri, context)
                                    count++
                                    if (bitmap != null) {
                                        //  launch(exceptionHandler + Dispatchers.IO) {
                                        if (mediaHandler.saveImage(
                                                it.uri,
                                                bitmap = bitmap,
                                                format = saveFormat.format,
                                                relativePath = Environment.DIRECTORY_PICTURES + "/" + CUSTOM_FOLDER_NAME,
                                                displayName = media.imageName
                                                    ?: "imageResizer_${
                                                        media.key
                                                    }.jpg",
                                                mimeType = saveFormat.mimeType
                                            ) == null
                                        ) {
                                            log("mediaHandler.saveImage failed for $scaledUri")
                                        } else {
                                            processed.add(media)


                                        }
                                        //   }

                                    } else {
                                        log("null bitmap for $scaledUri")
                                    }
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        defList.add(def)

                    }
                    println(" ${savingState.value} size here")



                    defList.awaitAll()


                    delay(1000)
                    mutex.withLock {
                        _savingState.value = processed.size
                    }
                    log("waited for  list $count")
                }.collect {

                }
                //joinAll(*saveJobs.toTypedArray()) //Wait for each task to be finished.
            } catch (e: Exception) {
                _isSaving.value = false
                onFail(e.message ?: "Unable to save some images").also { _isSaving.value = false }
                return@launch

            }
            _isSaving.value = false
            onSuccess("Images saved")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun saveOverride(
        context: Context,
        saveFormat: SaveFormat = SaveFormat.JPEG,
        onSuccess: (String) -> Unit = {},
        onFail: (String) -> Unit = {}
    ) {
        log("saveOverride")
        val exceptionHandler = CoroutineExceptionHandler { _, e ->
            println("[ERROR] ${e.message}")
        }
        val mutex = Mutex()
        _isSaving.value = true
        val currentSelectedItems = _scaledImageItems.value
        _savingState.value = 0
        val processed = mutableListOf<ImageItem>()
        var count = 0
        //Check if the space is enough
        if (!isEnoughSpaceAvailable(currentSelectedItems.size)) {
            _isSaving.value = false
            onFail("Not enough space available").also { _isSaving.value = false }
            println("Not enough space available")
            return
        }

        job = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                _savingState.value = 2
                currentSelectedItems.asFlow().flowOn(Dispatchers.IO).chunked(30).map { list ->
                    println("wait for this list $count")
                    val defList = mutableListOf<Deferred<Any?>>()
                    list.mapIndexed { index, it ->
                        val def = async(exceptionHandler + Dispatchers.IO) {
                            yield()

                            //   mutex.withLock {
                            try {
                                //   val saveJobs =   currentSelectedItems.mapIndexed { index, it ->
                                val media = it
                                it.scaledUri?.let { scaledUri ->
                                    var bitmap: Bitmap? = null
                                    bitmap = loadBitmapFromUri(scaledUri, context)
                                    count++
                                    if (bitmap != null) {
                                        //  launch(exceptionHandler + Dispatchers.IO) {
                                        if (!mediaHandler.overrideImage(
                                                originalUri = scaledUri,
                                                uri = media.uri,
                                                bitmap = bitmap,
                                                format = saveFormat.format
                                            )
                                        ) {
                                            println("mediaHandler.overrideImage failed for $scaledUri")
                                        } else {
                                            processed.add(media)
                                        }
                                        //   }

                                    } else {
                                        println("null bitmap for $scaledUri")
                                    }
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        defList.add(def)
                    }
                    println(" ${savingState.value} size here")
                    _savingState.value = processed.size
                    println("waiting for  ${defList.size} to finish")
                    defList.awaitAll()
                    delay(1000)
                    println("waited for  list $count")
                }.collect {

                }
                //joinAll(*saveJobs.toTypedArray()) //Wait for each task to be finished.
            } catch (e: Exception) {
                _isSaving.value = false
                onFail(e.message ?: "Unable to save some images").also { _isSaving.value = false }
                return@launch

            }
            _isSaving.value = false
            onSuccess("Images saved")
        }

    }


    fun onSelectedItemClicked(item: ImageItem, navigate: (String) -> Unit) {
        selectedItem = item
        navigate(Screen.ZoomableScreen.route)
    }


    fun onImageItemClicked(item: ImageItem, navigate: (String) -> Unit) {
        selectedItem = item
        navigate(Screen.ZoomableScreen.route)
    }

}
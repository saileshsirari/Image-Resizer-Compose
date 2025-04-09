package com.image.resizer.compose

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.image.resizer.compose.ImageSaveWorker.Companion.KEY_IMAGE_URIS
import com.image.resizer.compose.ImageSaveWorker.Companion.KEY_PROCESSED_COUNT
import com.image.resizer.compose.ImageSaveWorker.Companion.KEY_SAVE_FORMAT
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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlin.text.format
import kotlin.time.Duration
import kotlin.time.measureTime


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
    private val _isSaving = MutableStateFlow(false)
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

    private var _replacingState = MutableStateFlow<Int>(0)
    var replacingState = _replacingState.asStateFlow()

    init {
        log("HomeScreenViewModel init")
        log(
            "_isSaving ${_isSaving.value} selected items ${_selectedImageItems.value.size} scaled items ${_scaledImageItems.value.size} " +
                    "saving state ${savingState.value}" +
                    ""
        )
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
        size: Int = 20
    ) {
        imageItems.take(size).forEachIndexed { index, it ->
            // viewModelScope.launch(Dispatchers.Default) {
            // println("$operation $index " + it.computeScaledUri())
            // }
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
                    uri = _selectedImageItems.value.first().uri,
                    computedUri = croppedUri,
                    imageName = requireNotNull(_selectedImageItems.value.first().imageName),
                    originalRelativePath = requireNotNull(_selectedImageItems.value.first().originalRelativePath),
                    originalMimeType = requireNotNull(_selectedImageItems.value.first().originalMimeType),
                    timestamp = _selectedImageItems.value.first().timestamp
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
                uri = it.uri,
                imageName = it.imageName,
                originalFileSize = it.originalFileSize ?: 0L,
                originalRelativePath = requireNotNull(it.originalRelativePath),
                originalMimeType = it.originalMimeType,
                originalImageDimension = it.originalImageDimension,
                timestamp = it.timestamp,
                path = it.path
            )
        }
        ImageItem.percentScale = null
        ImageItem.scaleParams = null
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
                uri = it.uri,
                imageName = it.imageName,
                originalFileSize = it.originalFileSize,
                originalRelativePath = it.originalRelativePath,
                originalMimeType = it.originalMimeType,
                originalImageDimension = it.originalImageDimension,
                timestamp = it.timestamp
            )
        }
        _selectedImageItems.value = selectedImageItems
        //onGalleryImagesSelected(context,selectedImageItems)
        ImageItem.percentScale = null
        ImageItem.scaleParams = null
    }

    fun onClear(context: Context) {
        log("onClear")
        _cropState.value = CropState.Idle
        _compressState.value = CompressState.Idle
        _scaleState.value = ScaleState.Idle
        _galleryState.value = GalleryState.Idle
        _scaledImageItems.value = emptyList<ImageItem>()
        _selectedImageItems.value = emptyList<ImageItem>()
        _replacingState.value = 0
        _savingState.value = 0
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

    var job: Deferred<Any?>? = null
    private val _event = MutableSharedFlow<Boolean>()
    val event = _event.asSharedFlow()
    private var isPaused = false

    fun cancelSave() {
        viewModelScope.launch {
            mutex.withLock {
                log("cancelSave")
                _isSaving.value = false
                job?.cancel()
                job = null
            }
        }
    }

    fun saveImagesWithWorkManager(
        context: Context,
        imageItems: List<ImageItem>,
        saveFormat: SaveFormat = SaveFormat.JPEG
    ) {
        val uris = imageItems.map { it.uri.toString() }.toTypedArray()
        // Create the input data for the worker
        val inputData = workDataOf(
            KEY_IMAGE_URIS to uris,
            KEY_SAVE_FORMAT to if (saveFormat == SaveFormat.JPEG) "JPEG" else "PNG"
        )
        // Create the WorkRequest
        val saveRequest = OneTimeWorkRequest.Builder(ImageSaveWorker::class.java)
            .setInputData(inputData)
            .build()

        // Enqueue the work
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(saveRequest)
        _isSaving.update { true }
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(saveRequest.id).filterNotNull().collect { workInfo ->
                if (workInfo.state == WorkInfo.State.RUNNING) {
                    val progress = workInfo.progress.getInt(KEY_PROCESSED_COUNT, 0)
                    _savingState.update { progress }

                } else if (workInfo.state.isFinished) {
                    _isSaving.update { false }
                }
            }
        }
    }

    val mutex = Mutex()
    val exceptionHandler = CoroutineExceptionHandler { _, e ->
        log("[ERROR] ${e.message}")
    }

    fun <T> divideListManually(list: List<T>, chunkSize: Int): List<List<T>> {
        val result = mutableListOf<List<T>>()
        var index = 0
        while (index < list.size) {
            result.add(list.subList(index, minOf(index + chunkSize, list.size)))
            index += chunkSize
        }
        return result
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun saveCopy(
        context: Context,
        saveFormat: SaveFormat = SaveFormat.JPEG,
        onSuccess: (String) -> Unit = {},
        onFail: (String) -> Unit = {}
    ) {
        log("saveCopy")
        val processed = mutableListOf<ImageItem>()

        job = viewModelScope.async(Dispatchers.Default + exceptionHandler) {
            _isSaving.value = true
            val currentSelectedItems =
                _scaledImageItems.value

            // Check if the space is enough
            if (!isEnoughSpaceAvailable(currentSelectedItems.size)) {
                _isSaving.value = false
                onFail("Not enough space available").also { _isSaving.value = false }
                println("Not enough space available")
                return@async
            }
            _savingState.value = 0
            try {
                ensureActive()
                val dividedList = divideListManually(currentSelectedItems, 20)
                dividedList.map { list ->
                    ensureActive()
                    log("list starting ${list.size}")

                    val saveJobs = list.map { media ->
                        async(exceptionHandler + Dispatchers.IO) {
                            ensureActive()
                            // mutex.withLock {
                            try {
                                if (media.scaledUri == null) {
                                    media.computeScaledUri(context)
                                }
                                media.scaledUri?.let { scaledUri ->
                                    var bitmap: Bitmap? = null
                                    bitmap = loadBitmapFromUri(scaledUri, context)
                                    if (bitmap != null) {
                                        if (mediaHandler.saveImage(
                                                media.uri,
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
                                            _savingState.value = processed.size
//                                                log("processed " + processed.size)
                                        }
                                    } else {
                                        log("null bitmap for $scaledUri")
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    val timeTaken: Duration = measureTime {
                        saveJobs.awaitAll()
                    }
                    log("Time taken for awaitAll(): ${timeTaken.inWholeSeconds} seconds")
                }


            } catch (e: Exception) {
                //_isSaving.value = false
                //  _savingState.value = 0
                log("Exception here " + e.message)
                onFail("Unable to replace some images").also {
                    _isSaving.value = false
                    _replacingState.value = 0
                    _savingState.value = 0
                }
                return@async
            }

        }
        println("All done")
        job?.await()
        _isSaving.value = false
        log("Images saved last")
        onSuccess("Images saved")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun saveOverride(
        context: Context,
        saveFormat: SaveFormat = SaveFormat.JPEG,
        onSuccess: (String) -> Unit = {},
        onFail: (String) -> Unit = {}
    ) {
        log("saveOverride")
        log("saveCopy")
        val processed = mutableListOf<ImageItem>()

        job = viewModelScope.async(Dispatchers.Default + exceptionHandler) {
            _isSaving.value = true
            val currentSelectedItems =
                _scaledImageItems.value

            // Check if the space is enough
            if (!isEnoughSpaceAvailable(currentSelectedItems.size)) {
                _isSaving.value = false
                onFail("Not enough space available").also { _isSaving.value = false }
                println("Not enough space available")
                return@async
            }
            _replacingState.value = 0
            try {
                ensureActive()
                val dividedList = divideListManually(currentSelectedItems, 20)
                dividedList.map { list ->
                    ensureActive()
                    log("list starting ${list.size}")

                    val saveJobs = list.map { media ->
                        async(exceptionHandler + Dispatchers.IO) {
                            ensureActive()
                            // mutex.withLock {
                            try {
                                if (media.scaledUri == null) {
                                    media.computeScaledUri(context)
                                }
                                media.scaledUri?.let { scaledUri ->
                                    var bitmap: Bitmap? = null
                                    bitmap = loadBitmapFromUri(scaledUri, context)
                                    if (bitmap != null) {
                                        if (!mediaHandler.overrideImage(
                                                originalUri = scaledUri,
                                                uri = media.uri,
                                                bitmap = bitmap,
                                                displayName = requireNotNull(media.imageName),
                                                originalRelativePath = media.originalRelativePath,
                                                mimeType = media.originalMimeType,
                                                format = saveFormat.format,
                                                timestamp = media.timestamp
                                            )
                                        ) {
                                            mediaHandler.saveImage(
                                                media.uri,
                                                bitmap = bitmap,
                                                format = saveFormat.format,
                                                relativePath = media.originalRelativePath,
                                                displayName = media.imageName,
                                                mimeType = saveFormat.mimeType
                                            )
                                            log("mediaHandler.overrideImage failed for $scaledUri")
                                            cancelSave()
                                            throw Exception("mediaHandler.overrideImage failed for $scaledUri")
                                        } else {
                                            processed.add(media)
                                            _replacingState.value = processed.size
//                                                log("processed " + processed.size)
                                        }
                                    } else {
                                        log("null bitmap for $scaledUri")
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    val timeTaken: Duration = measureTime {
                        saveJobs.awaitAll()
                    }
                    log("Time taken for awaitAll(): ${timeTaken.inWholeSeconds} seconds")
                }


            } catch (e: Exception) {
                //_isSaving.value = false
                //  _savingState.value = 0
                log("Exception here " + e.message)
                onFail("Unable to replace some images").also {
                    _isSaving.value = false
                    _replacingState.value = 0
                    _savingState.value = 0
                }
                return@async
            }

        }
        println("All done")
        job?.await()
        _isSaving.value = false
        onClear(context)
        log("Images saved last")
        onSuccess("Images saved")
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
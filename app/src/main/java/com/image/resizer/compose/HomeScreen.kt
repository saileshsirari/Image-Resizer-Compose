@file:Suppress("DEPRECATION")

package com.image.resizer.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.image.resizer.compose.ImageReplacer.deleteImage
import com.image.resizer.compose.mediaApi.AlbumsViewModel
import com.image.resizer.compose.mediaApi.PickerMediaSheet
import com.image.resizer.compose.mediaApi.MediaHandleUseCase
import com.image.resizer.compose.mediaApi.MediaViewModel
import com.image.resizer.compose.mediaApi.NavigationButton
import com.image.resizer.compose.mediaApi.SaveFormat
import com.image.resizer.compose.mediaApi.model.AlbumState
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.model.MediaState
import com.image.resizer.compose.mediaApi.rememberAppBottomSheetState
import com.image.resizer.compose.mediaApi.util.printError
import com.image.resizer.compose.mediaApi.util.rememberActivityResult
import com.image.resizer.compose.mediaApi.util.writeRequests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun HomeScreenPreview1() {
//    HomeScreen()
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun <T : Media> HomeScreen(
    homeScreenViewModel: HomeScreenViewModel,
    albumsViewModel: AlbumsViewModel,
    timelineViewModel: MediaViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    navController: NavHostController,
    paddingValues: PaddingValues,
    mediaState: State<MediaState<Media.UriMedia>>,
    selectionState: MutableState<Boolean>,
    albumsState: State<AlbumState> = remember { mutableStateOf(AlbumState()) },
    selectedMedia: SnapshotStateList<T>,
    albumName: String = stringResource(R.string.app_name),
    navigate: (route: String) -> Unit,
    onItemClick: () -> Unit,
    handler: MediaHandleUseCase,
    navigateUp: @DisallowComposableCalls () -> Unit,
) {
// Preloaded viewModels
    val albumsState =
        albumsViewModel.albumsFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
    val timelineState =
        timelineViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
    // val selectedUris = homeScreenViewModel.selectedUris.collectAsStateWithLifecycle(emptyList())
    val copySheetState = rememberAppBottomSheetState()
    val context = LocalContext.current
    var scaledParams by remember { mutableStateOf(listOf<ScaleParams>()) }
    val viewModel = ScaleImageViewModel()
    // State to control the popup's visibility
    val galleryPermissionState = rememberPermissionState(
        getStoragePermission()
    )
    var showRationale by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(true) }
//    var imagesDimensions by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }
    //states
    val cropState by homeScreenViewModel.cropState.collectAsState()
    val compressState by homeScreenViewModel.compressState.collectAsState()
    val scaleState by homeScreenViewModel.scaleState.collectAsState()
    val galleryState by homeScreenViewModel.galleryState.collectAsState()
    val showToast by homeScreenViewModel.showToast.collectAsState()
    val scope = rememberCoroutineScope()


    val showImages by remember {
        derivedStateOf { galleryState is GalleryState.Success }
    }
    val imagesTransformed by remember {
        derivedStateOf {
            cropState is CropState.Success || scaleState is ScaleState.Success ||
                    compressState is CompressState.Success
        }
    }
    var saveRequested by remember { mutableStateOf(false) }

    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val croppedBitmapUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(CropScreen.CROPPED_IMAGE_BITMAP_URI, Uri::class.java)
            } else {
                data?.getParcelableExtra<Uri>(CropScreen.CROPPED_IMAGE_BITMAP_URI)
            }
            homeScreenViewModel.onCropSuccess(croppedBitmapUri!!)
        }

    }



    Scaffold(
        topBar = {
            HomeScreenTopAppBar(
                imagesTransformed = imagesTransformed,
                galleryState = galleryState,
                onShowScalePopup = {
                    homeScreenViewModel.onShowScalePopup()
                },
                onCrop = { show, uri ->
                    homeScreenViewModel.onShowCropPopup()
                },
                onUndo = {
                    homeScreenViewModel.onUndo()
                }, onShowCompress = {
                    homeScreenViewModel.onShowCompressPopup()
                },
                navigateUp = navigateUp,
                albumName = albumName,
                selectedMedia = selectedMedia,
                selectionState = selectionState
            )
        },
        floatingActionButton = {
            // Custom position for the FloatingActionButton
            Box(modifier = Modifier.fillMaxSize()) {
                PickerMediaSheet(
                    sheetState = copySheetState,
                    mediaList = selectedMedia,
                    albumsState = albumsState,
                    paddingValues = paddingValues,
                    mediaState = mediaState,
                    homeScreenViewModel = homeScreenViewModel,
                    activity = context as Activity
                )
                FloatingActionButton(
                    onClick = {
                        showDialog = true
                        if (galleryPermissionState.status.isGranted) {
                            if (albumsState.value.albums.isNotEmpty()) {
                                scope.launch {
                                    copySheetState.show()
                                }

                            } else {
                                homeScreenViewModel.showToast("No Pictures found")
                            }

                            //  onItemClick()
                            /*    multiplePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )*/
                        } else {
                            showRationale = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    Icon(Icons.Filled.Add, "Select Images")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(innerPadding)

        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 64.dp), // Add padding at the bottom for the FAB
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                HandleGalleryState(galleryState, showImages)


                val currentCropState = cropState
                when (currentCropState) {
                    is CropState.PopupShown -> {
                        Log.d(TAG, "CropState.PopupShown")
                        if (homeScreenViewModel.selectedImageItems.isNotEmpty()) {
                            val intent = Intent(context, CropScreen::class.java)
                            intent.putExtra(
                                CropScreen.IMAGE_TO_CROP,
                                homeScreenViewModel.selectedImageItems.first().uri
                            )
                            cropImageLauncher.launch(intent)
                            homeScreenViewModel.onCropScreenLaunched()
                        }
                    }

                    is CropState.Success -> {
                        currentCropState.data.croppedImageUri?.let {
                            CroppedImageComponent(currentCropState.data.croppedImageUri) {
                                getBitmapFromUri(
                                    currentCropState.data.croppedImageUri,
                                    context
                                )?.let {
                                    saveImagesToGallery(
                                        context,
                                        listOf(
                                            ImageItem(
                                                uri = currentCropState.data.croppedImageUri,
                                                scaledBitmap = it
                                            )
                                        )
                                    )
                                    homeScreenViewModel.showToast()
                                    homeScreenViewModel.showSelectedImages()
                                }
                            }
                        }
                    }

                    is CropState.Loading -> {
                    }

                    is CropState.Error -> {
                        Text("Crop error: ${currentCropState.message}")
                    }

                    is CropState.Idle -> {

                    }
                }
                val currentScaleState = scaleState
                when (currentScaleState) {
                    is ScaleState.Idle, is ScaleState.Loading -> {

                    }

                    is ScaleState.ShowPopup -> {
                        val originalDimensions =
                            homeScreenViewModel.selectedImageItems.mapNotNull { it.imageDimension }
                        // Implement image scaling logic here
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(durationMillis = 2000)) + expandVertically(
                                expandFrom = Alignment.Companion.CenterVertically,
                                animationSpec = tween(durationMillis = 1300)
                            ),
                            exit = fadeOut(animationSpec = tween(durationMillis = 2000)) + shrinkVertically(
                                shrinkTowards = Alignment.Companion.CenterVertically,
                                animationSpec = tween(durationMillis = 1300)
                            ),
                        ) {
                            ScaleImagePopup(true, onDismiss = {
                                homeScreenViewModel.dismissScalePopup()
                            }, originalDimensions, viewModel = viewModel, onScale = {
                                //  it.forEachIndexed { index, it ->
                                //  Log.d(TAG, " $it here  ${originalDimensions[index]} ")
                                //  }
                                // showScaledImages = true
                                scaledParams = it
                                homeScreenViewModel.onImagesScaled(it)
                            })
                        }
                    }

                    is ScaleState.Success -> {
                        if (homeScreenViewModel.selectedImageItems.isNotEmpty()) {
                            val imageItems =
                                homeScreenViewModel.selectedImageItems
                            ScaledImageScreen(
                                imageItems = imageItems,
                                currentScaleState.data.scaleParamsList,
                                onSaveClicked = {
                                    saveRequested = true

                                })
                        }

                    }

                    is ScaleState.Error -> {

                    }

                }
                val currentCompressState = compressState
                HandleCompressState(
                    currentCompressState,
                    homeScreenViewModel,
                )
            }
        }
    }

    // Conditionally display the toast
    if (showToast.isNotEmpty()) {
        LaunchedEffect(key1 = true) {
            Toast.makeText(context, showToast, Toast.LENGTH_SHORT).show()
            homeScreenViewModel.showToast("") // Reset the state after showing the toast
        }
    }
    if (saveRequested) {
        val overrideRequest = overrideImagesRequest(homeScreenViewModel, context)
        LaunchedEffect(key1 = true) {
            homeScreenViewModel.selectedImageItems.let {
                overrideRequest.launch(
                    it.map { it.uri }
                        .writeRequests((context as Activity).contentResolver)
                )
            }

            /*  homeScreenViewModel.showToast()

              homeScreenViewModel.saveOverride(onSuccess = {
                  homeScreenViewModel.showToast()
                  homeScreenViewModel.showSelectedImages()
              }, onFail = {
                  homeScreenViewModel.showToast("Failed")
              })*/
        }
    }
    if (showDialog) {


    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                showRationale = false
            },
            title = { Text("Permission Required") },
            text = { Text("The app needs permission to access your gallery.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        galleryPermissionState.launchPermissionRequest()
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

}


@Composable
private fun HandleCompressState(
    currentCompressState: CompressState,
    homeScreenViewModel: HomeScreenViewModel,
) {
    var deleteImages by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    if (deleteImages) {

        deleteImage(
            context,
            homeScreenViewModel.selectedImageItems.map { it.uri }.first(),
            onDeleted = {


            })
        deleteImages = false

    }

    val overrideRequest = overrideImagesRequest(homeScreenViewModel, context)
    when (currentCompressState) {
        is CompressState.Success -> {
            if (homeScreenViewModel.selectedImageItems.isNotEmpty()) {
                CompressToKbImageScreen(
                    imageItems = homeScreenViewModel.selectedImageItems,
                    sizeInKb = currentCompressState.data.size,
                    onSaveClicked = {
//                        deleteImages =true
                        saveImagesToGallery(context, it)
                        homeScreenViewModel.showToast()
                        homeScreenViewModel.showSelectedImages()
                    },
                    onSReplaceClicked = {
                        overrideRequest.launch(
                            it.map { it.uri }
                                .writeRequests((context as Activity).contentResolver)
                        )

                        /*   it.forEach { imageItem ->
                               imageItem.scaledBitmap?.let {
                                   scope.launch {
                                       imageItem.uri.let { uri -> overrideRequest.launch(
                                           uri.writeRequest( (context as Activity).contentResolver)) }
                                   }

                               }
                           }*/

                    })
            }
        }

        is CompressState.ImagesSaved -> {
            homeScreenViewModel.showSelectedImages()
        }

        is CompressState.PopupShown -> {
            CompressDialog(onDismiss = {
                homeScreenViewModel.onCompressCancel()
            }, onConfirm = {
                homeScreenViewModel.onCompressShowImages(it)
            })
        }

        is CompressState.Idle -> {

        }

        is CompressState.Error -> {
            Text("Compress error: ${currentCompressState.message}")
        }
    }
}

@Composable
private fun overrideImagesRequest(
    homeScreenViewModel: HomeScreenViewModel,
    context: Context
): ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult> {
    val overrideRequest = rememberActivityResult(
        onResultOk = {
            var replaced = false
            homeScreenViewModel.selectedImageItems.forEach { imageItem ->
                imageItem.scaledBitmap?.let {
                    replaced = ImageReplacer.replaceOriginalImageWithBitmap(
                        context,
                        imageItem.uri,
                        it
                    )

                }
            }
            if (replaced) {
                homeScreenViewModel.showToast("Images replaced")
            } else {
                homeScreenViewModel.showToast("Error in replacing images  ")
            }
        }
    )
    return overrideRequest
}

@Composable
private fun HandleGalleryState(
    galleryState: GalleryState,
    showImages: Boolean
) {
    val currentGalleryState = galleryState
    when (currentGalleryState) {
        is GalleryState.Success -> {
            AnimatedVisibility(
                visible = showImages,
                enter = fadeIn(animationSpec = tween(durationMillis = 3000)),
                exit = fadeOut(animationSpec = tween(durationMillis = 3000))
            ) {
                val data = currentGalleryState.data
                GalleryImagesComponent(data.imageItems)//can get data from gallery state
            }
        }

        is GalleryState.Error, GalleryState.Idle, GalleryState.Loading -> {
            AnimatedVisibility(
                visible = showImages,
                enter = fadeIn(animationSpec = tween(durationMillis = 3000)),
                exit = fadeOut(animationSpec = tween(durationMillis = 3000))
            ) {
                val data = emptyList<Uri>()
                GalleryImagesComponent(data)//can get data from gallery state
            }
        }

    }
}

@Composable
fun CompressToKbImageScreen(
    imageItems: List<ImageItem>,
    sizeInKb: Int = 100,
    onSaveClicked: (List<ImageItem?>) -> Unit,
    onSReplaceClicked: (List<ImageItem>) -> Unit,
) {
    var imagesScaled by remember { mutableStateOf(false) }
    var scaledImages by remember { mutableStateOf(listOf<ImageItem>()) }
    val context = LocalContext.current

    LaunchedEffect(sizeInKb) {
        imagesScaled = false
        withContext(Dispatchers.IO) {
            val imageScalar = ImageScalar(context)
            val scaledUris = withContext(Dispatchers.IO) {
                imageScalar.compressImagesToTargetSize(imageItems, sizeInKb = sizeInKb)
            }
            imagesScaled = true
            scaledImages = scaledUris.filterNotNull()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            if (imagesScaled) {

                ScaledImagesGrid(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp), scaledImages, imageItems
                )
            } else {
                Text("Scaling...")
            }
        }
        // Save button at the bottom, above the FAB
        if (imagesScaled) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp) // Padding around the button
            ) {
                Button(
                    onClick = {
                        onSaveClicked(scaledImages)
                    },
                ) {
                    Text(text = "Save Images")
                }

                Button(
                    onClick = {
                        onSReplaceClicked(scaledImages)
                    },
                ) {
                    Text(text = "Replace Images")
                }

            }
        }
    }
}


@Composable
fun CroppedImageComponent(uri: Uri, onSaveClicked: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center // Center content in the Box
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, // Align items horizontally
            verticalArrangement = Arrangement.spacedBy(16.dp) // Add vertical spacing between the image and button
        ) { // Centering the content within the column
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.Companion.weight(4f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Column(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = {
                        onSaveClicked()
                    }
                ) {
                    Text(text = "Save Cropped Image")
                }
            }
        }

    }
}


@Composable
private fun GalleryImagesComponent(selectedImageUris: List<Uri>) {
    val columns = if (selectedImageUris.size > 1) {
        GridCells.Fixed(2)
    } else {
        GridCells.Fixed(1)
    }
    LazyVerticalGrid(
        columns = columns,
        contentPadding = PaddingValues(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(selectedImageUris) { uri ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.Companion
                        .align(Alignment.CenterHorizontally)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .sizeIn(minWidth = 100.dp, minHeight = 200.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(1.dp))

                )
            }
        }
    }
}


@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenTopAppBarPreview() {
    val galleryState = GalleryState.Success(GalleryStateData(listOf()))
    val imagesTransformed = true
    val onUndo: () -> Unit = {

    }
    val onShowScalePopup: () -> Unit = {

    }

    val onCrop: (Boolean, Uri?) -> Unit = { _, _ ->

    }
    val onShowCompress: () -> Unit = {

    }
    /*    HomeScreenTopAppBar(
            imagesTransformed = imagesTransformed,
            galleryState = galleryState,
            onUndo = onUndo,
            onShowScalePopup = onShowScalePopup,
            onCrop = onCrop,
            onShowCompress = onShowCompress
        )*/
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Media> HomeScreenTopAppBar(
    imagesTransformed: Boolean = false,
    galleryState: GalleryState,
    onUndo: () -> Unit,
    onShowScalePopup: () -> Unit,
    onCrop: (Boolean, Uri?) -> Unit,
    onShowCompress: () -> Unit,
    albumId: Long = -1L,
    target: String? = remember { null },
    albumName: String = stringResource(R.string.app_name),
    navigateUp: () -> Unit,
    selectionState: MutableState<Boolean>,
    selectedMedia: SnapshotStateList<T>,
) {
    LargeTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary
        ),
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                /* TwoLinedDateToolbarTitle(
                     albumName = albumName,
                     dateHeader =  mediaState.value.dateHeader
                 )*/
                Text(
                    text = "Image Resizer",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier
                        .padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween

                ) {

                    when (galleryState) {
                        is GalleryState.Success -> {
                            val selectedImageUris = galleryState.data.imageItems
                            if (selectedImageUris.isNotEmpty()) {
                                ActionButtonWithText(
                                    enabled = true,
                                    onClick = {
                                        onShowCompress()
                                    },
                                    iconId = R.drawable.ic_compress_24dp,
                                    modifier = Modifier.padding(end = 10.dp),
                                    text = "Compress"
                                )
                                ActionButtonWithText(
                                    onClick = {
                                        onShowScalePopup()
                                    },
                                    enabled = true,
                                    iconId = R.drawable.ic_scale_24dp,
                                    modifier = Modifier.padding(end = 10.dp),
                                    text = "Scale"
                                )
                                if (selectedImageUris.size == 1) {
                                    ActionButtonWithText(
                                        onClick = {
                                            onCrop(
                                                true, selectedImageUris.first().uri
                                            )
                                        },
                                        iconId = R.drawable.ic_crop_24dp,
                                        modifier = Modifier.padding(end = 10.dp),
                                        text = "Crop"
                                    )
                                }

                            }
                        }

                        is GalleryState.Error -> {

                        }

                        GalleryState.Idle -> {

                        }

                        GalleryState.Loading -> {

                        }
                    }


                    if (imagesTransformed) {
                        ActionButtonWithText(
                            onClick = {
                                onUndo()
                            },
                            iconId = R.drawable.ic_undo_24dp,
                            modifier = Modifier.padding(end = 15.dp),
                            text = "Undo"
                        )
                    }
                }
            }
        },
        navigationIcon = {
            NavigationButton(
                albumId = albumId,
                target = target,
                navigateUp = navigateUp,
                clearSelection = {
                    selectionState.value = false
                    selectedMedia.clear()
                },
                selectionState = selectionState,
                alwaysGoBack = true,
            )
        }
    )
}


@Composable
fun ActionButtonWithText(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    iconId: Int,
    text: String
) {
    Column(
        modifier = modifier
            .padding(horizontal = 0.dp, vertical = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        IconButton(
            onClick = { onClick() },
            enabled = enabled,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription = null,
            )
        }
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = text,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


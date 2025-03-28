@file:Suppress("DEPRECATION")

package com.image.resizer.compose

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import apps.sai.com.imageresizer.R
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.image.resizer.compose.ImageReplacer.deleteSelectedImages
import com.image.resizer.compose.mediaApi.AlbumsViewModel
import com.image.resizer.compose.mediaApi.EditorDestination.*
import com.image.resizer.compose.mediaApi.EditorDestination.ExternalEditor
import com.image.resizer.compose.mediaApi.EditorNavigator
import com.image.resizer.compose.mediaApi.MediaHandleUseCase
import com.image.resizer.compose.mediaApi.NavigationButton
import com.image.resizer.compose.mediaApi.PickerMediaSheet
import com.image.resizer.compose.mediaApi.TAG
import com.image.resizer.compose.mediaApi.model.AlbumState
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.model.MediaState
import com.image.resizer.compose.mediaApi.rememberAppBottomSheetState
import com.image.resizer.compose.mediaApi.util.Constants.Animation.enterAnimation
import com.image.resizer.compose.mediaApi.util.Constants.Animation.exitAnimation
import com.image.resizer.compose.mediaApi.util.rememberActivityResult
import com.image.resizer.compose.mediaApi.util.writeRequests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


@Composable
fun HomeScreenPreview1() {
//    HomeScreen()
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class,
    ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun <T : Media> HomeScreen(
    homeScreenViewModel: HomeScreenViewModel,
    albumsViewModel: AlbumsViewModel,
    paddingValues: PaddingValues,
    mediaState: State<MediaState<Media.UriMedia>>,
    selectionState: MutableState<Boolean>,
    selectedMedia: SnapshotStateList<T>,
    albumName: String = stringResource(R.string.app_name),
    navigate: (route: String) -> Unit,
    onItemClick: () -> Unit,
    albumsState: State<AlbumState>,
    handler: MediaHandleUseCase,
    navController: NavHostController,
    navigateUp: @DisallowComposableCalls () -> Unit,
) {
// Preloaded viewModels
    val copySheetState = rememberAppBottomSheetState()

    val navigator = rememberSupportingPaneScaffoldNavigator()

    val context = LocalContext.current
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

    var saveRequested by remember { mutableStateOf(false) }
    val selectedImageItems = homeScreenViewModel.selectedImageItems.collectAsState()
    var savingState = homeScreenViewModel.savingState.collectAsState()
    var isSaving = homeScreenViewModel.isSaving.collectAsState()

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
            homeScreenViewModel.onCropSuccess(context,croppedBitmapUri!!)
        }

    }
    val overrideRequest = rememberActivityResult(
        onResultOk = {
            var replaced = false
            homeScreenViewModel.saveOverride(context = context, onSuccess = {
                homeScreenViewModel.showToast("Images replaced")

            }, onFail = {
                homeScreenViewModel.showToast("Error in replacing images  ")

            })
        }

    )
    LaunchedEffect(scaleState, galleryState, cropState, compressState) {
        Log.d(TAG, "HomeScreen: scale =$scaleState, cropState = $cropState, compress =$compressState, gallery=$galleryState ")

    }
    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible =
                    galleryState is GalleryState.Success || cropState is CropState.Success
                            || scaleState is ScaleState.Success || compressState is CompressState.Success,
                enter = enterAnimation,
                exit = exitAnimation
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                stiffness = Spring.StiffnessHigh,
                                visibilityThreshold = IntSize.VisibilityThreshold
                            )
                        )
                ) {
                    AnimatedVisibility(
                        visible =
                            navigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting] == PaneAdaptedValue.Hidden,
                        enter = enterAnimation,
                        exit = exitAnimation
                    ) {
                        EditorNavigator(
                            modifier = Modifier
                                .fillMaxWidth(),
                            navController = navController,
                            targetImage = null,
                            targetUri = null,
                            startCropping = {
                            },
                            homeScreenViewModel = homeScreenViewModel,
                            onItemClick = {
                                when (it) {
                                    Compress -> {
                                        homeScreenViewModel.onShowCompressPopup(context)
                                    }

                                    Scale -> {
                                        homeScreenViewModel.onShowScalePopup(context)
                                    }

                                    Crop -> {
                                        homeScreenViewModel.onShowCropPopup(context)
                                    }

                                    Undo -> {
                                        homeScreenViewModel.onUndo()
                                    }

                                    Editor -> {

                                    }

                                    Save -> {
                                        homeScreenViewModel.saveCopy(
                                            context = context,
                                            onSuccess = {
                                                homeScreenViewModel.showToast()
                                            },
                                            onFail = {
                                                homeScreenViewModel.showToast("Failed")
                                            })

                                    }

                                    Replace -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            selectedImageItems.value.let {
                                                overrideRequest.launch(
                                                    it.map { it.uri }
                                                        .writeRequests((context as Activity).contentResolver)
                                                )
                                            }
                                        } else {
                                            homeScreenViewModel.saveOverride(
                                                context = context,
                                                onSuccess = {
                                                    homeScreenViewModel.showToast()
                                                    homeScreenViewModel.showSelectedImages()
                                                    saveRequested = false
                                                },
                                                onFail = {
                                                    homeScreenViewModel.showToast("Failed")
                                                    saveRequested = false
                                                })

                                        }

                                    }

                                    ExternalEditor -> {

                                    }

                                }
                            },
                        )
                    }
                }
            }
        },

        floatingActionButton = {

            AnimatedVisibility(visible = albumsState.value.albums.isNotEmpty()) {
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
        }
    ) { innerPadding ->

        showRationale = !galleryPermissionState.status.isGranted
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()) {
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(innerPadding)

            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 14.dp), // Add padding at the bottom for the FAB
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    HandleGalleryState(
                        galleryState,
                        showImages,
                        homeScreenViewModel.selectedImageItems
                    )


                    val currentCropState = cropState
                    when (currentCropState) {
                        is CropState.PopupShown -> {
                            if (selectedImageItems.value.isNotEmpty()) {
                                val intent = Intent(context, CropScreen::class.java)
                                intent.putExtra(
                                    CropScreen.IMAGE_TO_CROP,
                                    selectedImageItems.value.first().uri
                                )
                                cropImageLauncher.launch(intent)
                                homeScreenViewModel.onCropScreenLaunched()
                            }
                        }

                        is CropState.Success -> {
                            currentCropState.data.croppedImageUri?.let {
                                CroppedImageComponent(currentCropState.data.croppedImageUri)
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
                            Log.d(TAG,"ScaleState.ShowPopup here")
                            // Implement image scaling logic here
                                ScaleImagePopup(onDismiss = {
                                    homeScreenViewModel.dismissScalePopup()
                                }, viewModel = viewModel, onScale = {
                                    //  it.forEachIndexed { index, it ->
                                    //  Log.d(TAG, " $it here  ${originalDimensions[index]} ")
                                    //  }
                                    // showScaledImages = true
                                    homeScreenViewModel.onImagesScaled(context,it)
                                })
                        }

                        is ScaleState.Success -> {


                            if (selectedImageItems.value.isNotEmpty()) {
                                val imageItems =
                                    selectedImageItems.value
                                ScaledImagesGrid(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 10.dp),
                                    homeScreenViewModel = homeScreenViewModel,
                                    imageItems = imageItems,
                                    onSelectedItemClicked = {
                                        homeScreenViewModel.onSelectedItemClicked(it) {
                                            navController.navigate(it) {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )

                            }

                        }

                        is ScaleState.Error -> {

                        }

                    }
                    val currentCompressState = compressState
                    HandleCompressState(
                        currentCompressState = currentCompressState,
                        homeScreenViewModel = homeScreenViewModel,
                        navController = navController,
                        selectedImageItems = selectedImageItems.value,
                    )

                }
            }
            if (isSaving.value && savingState.value.isNotEmpty()) {
                val animatedProgress by animateFloatAsState(
                    targetValue = savingState.value.size.toFloat(),
                    animationSpec = tween(durationMillis = 10), // Animation duration
                    label = "slider animation"
                )
                AnimatedVisibility(isSaving.value) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp).align(Alignment.BottomCenter)
                        .background( MaterialTheme.colorScheme.background)) {
                        Slider(
                            value = animatedProgress,
                            onValueChange = { },
                            enabled = false,
                            modifier = Modifier
                                .padding(bottom = 8.dp),
                            valueRange = 0f..selectedImageItems.value.size.toFloat()
                        )
                        Text(
                            "Saving: ${(animatedProgress/selectedImageItems.value.size.toFloat())*100}%"
                        )

                    }
                }
            }
        }
    }

    // Conditionally display the toast
    if (showToast.isNotEmpty()) {
        LaunchedEffect(true) {
            Toast.makeText(context, showToast, Toast.LENGTH_SHORT).show()
            homeScreenViewModel.showToast("") // Reset the state after showing the toast
        }
    }


    if(showRationale) {
        showRationale =  StoragePermissionDialog( galleryPermissionState)
    }

}




@Composable
private fun HandleCompressState(
    currentCompressState: CompressState,
    homeScreenViewModel: HomeScreenViewModel,
    navController: NavHostController,
    selectedImageItems: List<ImageItem>
) {
    var deleteImages by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    var deletePendingUris by remember {
        mutableStateOf<List<Uri>>(
            emptyList()
        )
    }
    var deletePendingRecoverableSecurityException by remember {
        mutableStateOf<RecoverableSecurityException?>(
            null
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Retry deletion for all uris
            if (deletePendingRecoverableSecurityException != null) {
                deletePendingRecoverableSecurityException?.let { exception ->
                    deletePendingUris.forEach { uri ->
                        try {
                            context.contentResolver.delete(uri, null, null)
                        } catch (e: SecurityException) {
                            // Handle any further errors (e.g., log them)
                            e.printStackTrace()
                        }
                    }
                }
            }

        } else {
            // Handle failure or user cancellation
            Log.e("MyImagesScreen", "Deletion failed or cancelled by user")
        }
        deletePendingUris = emptyList()
        deletePendingRecoverableSecurityException = null
    }

    if (deleteImages) {
        deleteSelectedImages(
            true,
            context,
            result = launcher,
            selectedImages = selectedImageItems.map { it.uri })
        deleteImages = false
    }

    when (currentCompressState) {
        is CompressState.Success -> {
            if (selectedImageItems.isNotEmpty()) {
                ScaledImagesGrid(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 10.dp),
                    homeScreenViewModel = homeScreenViewModel,
                    imageItems = selectedImageItems,
                    onSelectedItemClicked = {
                        homeScreenViewModel.onSelectedItemClicked(it) {
                            navController.navigate(it) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )

            }
        }

        is CompressState.ImagesSaved -> {
            homeScreenViewModel.showSelectedImages()
        }

        is CompressState.PopupShown -> {
            CompressDialog(onDismiss = {
                homeScreenViewModel.onCompressCancel(context)
            }, onConfirm = {
                homeScreenViewModel.onCompressShowImages(context, it)
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
private fun HandleGalleryState(
    galleryState: GalleryState,
    showImages: Boolean,
    selectedImageItems: StateFlow<List<ImageItem>>
) {
    val currentGalleryState = galleryState
    when (currentGalleryState) {
        is GalleryState.Success -> {
            AnimatedVisibility(
                visible = showImages,
                enter = fadeIn(animationSpec = tween(durationMillis = 3000)),
                exit = fadeOut(animationSpec = tween(durationMillis = 3000))
            ) {
                GalleryImagesComponent(selectedImageItems)//can get data from gallery state
            }
        }

        is GalleryState.Error, GalleryState.Idle, GalleryState.Loading -> {
            AnimatedVisibility(
                visible = showImages,
                enter = fadeIn(animationSpec = tween(durationMillis = 3000)),
                exit = fadeOut(animationSpec = tween(durationMillis = 3000))
            ) {
                val data = emptyList<Uri>()
                // GalleryImagesComponent(data)//can get data from gallery state
            }
        }

    }
}

@Composable
fun CompressToKbImageScreen(
    imageItems: List<ImageItem>,
    sizeInPercentage: Int = 100,
    homeScreenViewModel: HomeScreenViewModel,
    navController: NavHostController
) {

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


        }
    }
}


@Composable
fun CroppedImageComponent(uri: Uri) {
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
    albumId: Long = -1L,
    target: String? = remember { null },
    navigateUp: () -> Unit,
    selectionState: MutableState<Boolean>,
    selectedMedia: SnapshotStateList<T>,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary
        ),
        title = {
            Text(stringResource(R.string.app_name))
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


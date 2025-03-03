package com.image.resizer.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen() {
    val TAG = "HomeScreen"
    var selectedImageUris by remember { mutableStateOf(emptyList<Uri>()) }
    val context = LocalContext.current
    var imagePairs by remember { mutableStateOf(listOf<ImagePair>()) }
    var scaledParams by remember { mutableStateOf(listOf<ScaleParams>()) }
    val viewModel = ScaleImageViewModel()
    var scaledToKb by remember { mutableStateOf(listOf<ImagePair>()) }

    // State to control the popup's visibility
    val galleryPermissionState = rememberPermissionState(
        getStoragePermission()
    )
    var showRationale by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(true) }
    var showScalePopup by remember { mutableStateOf(false) }
    var showCompressPopup by remember { mutableStateOf(false) }
    var showCompressedImages by remember { mutableStateOf(false) }
    var showScaledImages by remember { mutableStateOf(false) }
    var imagesDimensions by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }

    var showCropDialog by remember { mutableStateOf(false) }
    var imageToCrop by remember { mutableStateOf<Uri?>(null) }
    var croppedBitmapUri by remember { mutableStateOf<Uri?>(null) }
    var showToast by remember { mutableStateOf(false) }
    var imagesTransformed by remember { mutableStateOf(false) }
    var sizeInKb by remember { mutableStateOf(100) }
    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                croppedBitmapUri =
                    data?.getParcelableExtra(CropScreen.CROPPED_IMAGE_BITMAP_URI, Uri::class.java)
            } else {
                croppedBitmapUri =
                    data?.getParcelableExtra<Uri>(CropScreen.CROPPED_IMAGE_BITMAP_URI)
            }
        }
        showCropDialog = false
    }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            selectedImageUris = uris
            imagesDimensions = selectedImageUris.map {
                imageDimensionsFromUri(context, ImageItem(it))
            }
        }
    )
    LaunchedEffect(imagePairs, showScaledImages, croppedBitmapUri) {
        imagesTransformed = imagePairs.isNotEmpty() || showScaledImages || croppedBitmapUri != null
    }
    Scaffold(
        topBar = {
            HomeScreenTopAppBar(
                imagesTransformed = imagesTransformed,
                selectedImageUris = selectedImageUris,
                onShowScalePopup = {
                    showScalePopup = !showScalePopup
                    //scale = true
                },
                onCrop = { show, uri ->
                    showCropDialog = show
                    if (selectedImageUris.isNotEmpty()) {
                        imageToCrop = uri
                        val intent = Intent(context, CropScreen::class.java)
                        intent.putExtra(CropScreen.IMAGE_TO_CROP, imageToCrop)
                        cropImageLauncher.launch(intent)
                    }
                },
                onUndo = {
                    imagePairs = emptyList()
                    showScaledImages = false
                    croppedBitmapUri = null
                }, onShowCompress = {
                    showCompressPopup = !showCompressPopup

                }

                /* ,  = { compressedUris ->
                     imagePairs = selectedImageUris.zip(compressedUris) { original, compressed ->
                         ImagePair(original, compressed)
                     }
                 }*/
            )
        },
        floatingActionButton = {
            // Custom position for the FloatingActionButton
            Box(modifier = Modifier.fillMaxSize()) {
                FloatingActionButton(
                    onClick = {
                        showDialog = true
                        if (galleryPermissionState.status.isGranted) {
                            multiplePhotoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
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
                if (croppedBitmapUri != null) {
                    croppedBitmapUri?.let { croppedBitmapUri ->
                        CroppedImageComponent(croppedBitmapUri) {
                            getBitmapFromUri(croppedBitmapUri, context)?.let {
                                saveImagesToGallery(
                                    context,
                                    listOf(ImageItem(uri = croppedBitmapUri, scaledBitmap = it))
                                )
                                showToast = true
                            }
                        }
                    }
                } else if (showCompressedImages) {
                    CompressToKbImageScreen(images = selectedImageUris, onSaveClicked = {
                        //  saveImagesToGallery(context,)
                    })
                } else if (showScaledImages) {
                    val imageItems = selectedImageUris.map { ImageItem(it) }
                    ScaledImageScreen(imageItems = imageItems, scaledParams, onSaveClicked = {
                        showScaledImages = false
                    })
                } else if (imagePairs.isNotEmpty()) {
                    ImageComparisonGrid(imagePairs)
                } else if (selectedImageUris.isNotEmpty()) {
                    GalleryImagesComponent(selectedImageUris)
                }

            }
        }
    }

    // Conditionally display the popup
    if (showToast) {
        LaunchedEffect(key1 = true) {
            Toast.makeText(context, "Image saved", Toast.LENGTH_SHORT).show()
            showToast = false // Reset the state after showing the toast
        }
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
    if (showCompressPopup) {
        CompressDialog(onDismiss = {
            showCompressPopup = false
        }, onConfirm = {
            showCompressPopup = false
            showCompressedImages = true
            sizeInKb = it


        })
    }

    if (showScalePopup) {
        val originalDimensions = imagesDimensions
        // Implement image scaling logic here
        AnimatedVisibility(
            visible = showScalePopup,
            enter = fadeIn(animationSpec = tween(durationMillis = 2000)) + expandVertically(
                expandFrom = Alignment.Companion.CenterVertically,
                animationSpec = tween(durationMillis = 1300)
            ),
            exit = fadeOut(animationSpec = tween(durationMillis = 2000)) + shrinkVertically(
                shrinkTowards = Alignment.Companion.CenterVertically,
                animationSpec = tween(durationMillis = 1300)
            ),
        ) {
            ScaleImagePopup(showScalePopup, onDismiss = {
                showScalePopup = false
            }, originalDimensions, viewModel = viewModel, onScale = {
                it.forEachIndexed { index, it ->
                    Log.d(TAG, " $it here  ${originalDimensions[index]} ")
                }
                showScaledImages = true
                scaledParams = it

            })
        }
    }


}

@Composable
fun CompressToKbImageScreen(
    images: List<Uri>,
    sizeInKb: Int = 100,
    onSaveClicked: (List<Uri>) -> Unit
) {
    var imagesScaled by remember { mutableStateOf(false) }
    var scaledImages by remember { mutableStateOf(listOf<Uri?>()) }
    val context = LocalContext.current
    LaunchedEffect(sizeInKb) {
        imagesScaled = false
        withContext(Dispatchers.IO) {
            val imageScaler = ImageScaler(context)
            val scaledUris = withContext(Dispatchers.IO) {
                imageScaler.scaleImagesToTargetSize(images, sizeInKb = sizeInKb)
            }
            imagesScaled = true
            scaledImages = scaledUris
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

            val imagePairs = scaledImages.filterNotNull().mapIndexed { index, uri ->
                ImagePair(originalUri = images[index], compressedUri = uri)
            }
            if (imagesScaled) {
                ImageComparisonGrid(imagePairs, onSaveClicked)
            }
        }

    }
}

@Composable
internal fun ImageComparisonGrid(
    imagePairs: List<ImagePair>,
    onSaveClicked: (List<Uri>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(imagePairs) { pair ->
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly // Distribute space evenly
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally // Center items horizontally
                    ) {
                        Text(
                            "Compressed",
                            textAlign = TextAlign.Center
                        )
                        AsyncImage(
                            model = pair.compressedUri,
                            contentDescription = "Compressed Image",
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .fillMaxWidth()
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally // Center items horizontally
                    ) {
                        Text(
                            "Original",
                            textAlign = TextAlign.Center
                        )
                        AsyncImage(
                            model = pair.originalUri,
                            contentDescription = "Original Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onSaveClicked(imagePairs.map { it.compressedUri }) }) {
            Text("Save Kb Scaled Images")
        }
    }
}

@Preview
@Composable
fun CroppedImageComponentPreview() {
    val sampleUri = "content://media/external/file/54".toUri()
    CroppedImageComponent(uri = sampleUri, onSaveClicked = {
        println("Save button clicked")
    })
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

@Preview
@Composable
fun GalleryImagesComponentPreview() {
    val sampleUris = listOf<Uri>(
        "content://media/external/file/54".toUri(),
        "content://media/external/file/54".toUri()
    )
    GalleryImagesComponent(selectedImageUris = sampleUris)
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
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.Companion
                    .padding(4.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(1.dp))

            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenTopAppBar(
    imagesTransformed: Boolean = false,
    selectedImageUris: List<Uri>,
    onUndo: () -> Unit,
    onShowScalePopup: () -> Unit,
    onCrop: (Boolean, Uri?) -> Unit,
    onShowCompress: () -> Unit,
) {

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary
        ),
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gallery App",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .then(
                            Modifier
                                .then(Modifier.requiredWidthIn(min = 1.dp))
                                .width(IntrinsicSize.Max)
                        )
                        .wrapContentWidth()
                        .fillMaxHeight()
                        .padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,// Changed here
                    horizontalArrangement = Arrangement.End// Changed here

                ) {
                    if (selectedImageUris.isNotEmpty()) {
                        ActionButtonWithText(
                            enabled = selectedImageUris.isNotEmpty(),
                            onClick = {
                                onShowCompress()

                            },
                            iconId = R.drawable.ic_compress_24dp,
                            modifier = Modifier.padding(end = 15.dp),
                            text = "Compress"
                        )
                        ActionButtonWithText(
                            onClick = {
                                onShowScalePopup()
                            },
                            enabled = selectedImageUris.isNotEmpty(),
                            iconId = R.drawable.ic_compress_24dp,
                            modifier = Modifier.padding(end = 15.dp),
                            text = "Scale"
                        )
                        if (selectedImageUris.size == 1) {
                            ActionButtonWithText(
                                onClick = {
                                    onCrop(
                                        true, selectedImageUris.first()
                                    )
                                },
                                iconId = R.drawable.ic_compress_24dp,
                                modifier = Modifier.padding(end = 15.dp),
                                text = "Crop"
                            )
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
    )
}


@Composable
fun ActionButtonWithText(
    enabled: Boolean = true,
    onClick: () -> Unit,
    iconId: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 0.dp, vertical = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
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
            text = text,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverflowMenu(
    imagesShow: Boolean = false,
    croppedImageUri: Uri? = null,
    context: Context,
    onUndo: () -> Unit,
    onCrop: (Boolean, Uri?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Box {
        Column(
            modifier = Modifier
                .padding(start = 10.dp, end = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
                Icon(

                    painterResource(id = R.drawable.ic_more_horiz_24dp),
                    contentDescription = "More",
                )
            }


            Text(
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp),
                text = "More",
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            /* DropdownMenuItem(
                 text = { Text("Crop") },
                 onClick = {
                     expanded = false
                     onCrop(
                         true, if (croppedImageUri!=null) {
                             croppedImageUri
                         } else null
                     )

                 },
                 leadingIcon = {
                     Icon(painterResource(id = R.drawable.ic_crop_24dp), "Crop")
                 },
                 enabled = croppedImageUri!=null
             )*/
            DropdownMenuItem(
                text = { Text("Undo") },
                onClick = {
                    expanded = false
                    onUndo()
                },
                leadingIcon = {
                    Icon(painterResource(id = R.drawable.ic_undo_24dp), "Undo")
                },
                enabled = imagesShow
            )
        }
    }
}
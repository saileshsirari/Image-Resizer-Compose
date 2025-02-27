package com.image.resizer.compose

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen() {
    val TAG = "HomeScreen"
    var selectedImageUris by remember { mutableStateOf(emptyList<Uri>()) }
    val context = LocalContext.current
    var imagePairs by remember { mutableStateOf(listOf<ImagePair>()) }
    var scaledParams by remember { mutableStateOf(listOf<ScaleParams>()) }
    val viewModel = ScaleImageViewModel()
    // State to control the popup's visibility
    val galleryPermissionState = rememberPermissionState(
        getStoragePermission()
    )
    var showRationale by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(true) }
    var showScalePopup by remember { mutableStateOf(false) }
    var showScaledImages by remember { mutableStateOf(false) }
    var imagesDimensions by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            selectedImageUris = uris
            imagesDimensions = selectedImageUris.map {
                imageDimensionsFromUri(context, ImageItem(it))
            }
        }
    )
    // State to pass to the popup
    Scaffold(
        topBar = {
            HomeScreenTopAppBar(
                selectedImageUris,
                context,
                viewModel,
                onShowScalePopup = {
                    showScalePopup = !showScalePopup
                },
                onUndo = {
                    imagePairs = emptyList()
                }
            ) { compressedUris ->
                imagePairs = selectedImageUris.zip(compressedUris) { original, compressed ->
                    ImagePair(original, compressed)
                }
            }
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
                        ///

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
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(bottom = 64.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Companion.CenterHorizontally
            ) {
                if (showScaledImages) {
                    val imageItems = selectedImageUris.map { ImageItem(it) }
                    ScaledImageScreen(imageItems = imageItems, scaledParams, onSaveClicked = {})
                } else
                    if (imagePairs.isNotEmpty()) {
                        ImageComparisonGrid(imagePairs)
                    } else if (selectedImageUris.isNotEmpty()) {
                        GalleryImagesComponent(selectedImageUris)
                    }

            }

        }
    }

    // Conditionally display the popup

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
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        //content://media/external/file/54
        items(selectedImageUris) { uri ->

            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.Companion
                    .padding(4.dp)
                    .size(200.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .border(
                        1.dp,
                        Color.Companion.Gray,
                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )

            )
        }
    }
}

@Preview
@Composable
fun HomeScreenTopAppBarPreview() {
    val context = LocalContext.current
    val viewModel = ScaleImageViewModel()
    val selectedImageUris = listOf<Uri>(
        "content://media/external/file/54".toUri(),
        "content://media/external/file/54".toUri()
    )

    HomeScreenTopAppBar(
        selectedImageUris = selectedImageUris,
        context = context,
        viewModel = viewModel,
        onUndo = {
            // Handle undo action here if needed
        },
        onShowScalePopup = {
            // Handle show scale popup action here if needed
        },
        onCompress = {
            // Handle compress action here if needed
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenTopAppBar(
    selectedImageUris: List<Uri>,
    context: Context,
    viewModel: ScaleImageViewModel,
    onUndo: () -> Unit,
    onShowScalePopup: () -> Unit,
    onCompress: (List<Uri>) -> Unit,
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
                    .height(IntrinsicSize.Min), // Changed here
                horizontalArrangement = Arrangement.Start, // Changed here
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gallery App",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp), // Changed here
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) // Changed here
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.then(
                        Modifier
                            .then(Modifier.requiredWidthIn(min = 1.dp))
                            .width(IntrinsicSize.Max)
                    )
                       .wrapContentWidth() // Changed here
                        .fillMaxHeight() // Changed here
                        .padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,// Changed here
                    horizontalArrangement = Arrangement.End// Changed here

                ) {
                    ActionButtonWithText( //Changed here
                        enabled = selectedImageUris.isNotEmpty() ,
                        onClick = {
                            val compressedUris = compressAndSaveImages(selectedImageUris, context)
                            onCompress(compressedUris)
                        },
                        iconId = R.drawable.ic_compress_24dp,
                        modifier=Modifier.padding(end = 8.dp),
                        text = "Compress"
                    ) //Changed here
                    ActionButtonWithText(
                        onClick = {
                            onShowScalePopup()
                        },
                        enabled = selectedImageUris.isNotEmpty(),
                        iconId = R.drawable.ic_compress_24dp,
                        modifier=Modifier.padding(end = 8.dp),
                        text = "Scale"
                    )
                    OverflowMenu(
                        selectedImageUris,
                        context,
                        onUndo,
                    )
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
    Column(modifier = modifier
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
fun OverflowMenu(selectedImageUris: List<Uri>, context: Context, onUndo: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Column(modifier = Modifier
            .padding(horizontal = 0.dp, vertical = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ){

                IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp),) {
                    Icon(

                        painterResource(id = R.drawable.ic_more_horiz_24dp),
                        contentDescription = "More",
                    )
                }


            Text(style = MaterialTheme.typography.titleSmall,
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
            DropdownMenuItem(
                text = { Text("Crop") },
                onClick = {
                    expanded = false
                    cropImage(selectedImageUris)
                },
                leadingIcon = {
                    Icon(painterResource(id = R.drawable.ic_crop_24dp), "Crop")
                }
            )
            DropdownMenuItem(
                text = { Text("Undo") },
                onClick = {
                    expanded = false
                    onUndo()
                },
                leadingIcon = {
                    Icon(painterResource(id = R.drawable.ic_undo_24dp), "Undo")
                }
            )
        }
    }
}
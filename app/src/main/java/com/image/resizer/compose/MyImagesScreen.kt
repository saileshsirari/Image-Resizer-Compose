package com.image.resizer.compose

import android.Manifest
import android.R.attr.checked
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.image.resizer.compose.ImageHelper.getRealCompressedImageUris
import com.image.resizer.compose.ImageReplacer.deleteSelectedImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.remove
import kotlin.math.min

data class PaginationState(
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
)

const val IMAGES_PER_PAGE = 6

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyImagesScreen() {
    val context = LocalContext.current

    val totalImagesCount by remember {
        mutableIntStateOf(getTotalTransformedImagesCount(context))
    }

    var loadingImages by remember {
        mutableStateOf(mutableSetOf<Int>())
    }
    val placeholders = remember {
        List(totalImagesCount) { null }
    }
    var actualImageItems by remember {
        mutableStateOf<List<ImageItem>>(emptyList())
    }
    val lazyGridState = rememberLazyGridState()
    LaunchedEffect(true) {
        val newImages = mutableSetOf<Int>()
        for (i in 1..totalImagesCount) {
            newImages.add(i)
        }
        loadingImages.addAll(newImages)
        val urisForPage = withContext(Dispatchers.IO) {
            getRealCompressedImageUris(context, newImages.toList()).filterNotNull()
        }
        val list = actualImageItems.toMutableList()
        list.addAll(urisForPage)
        actualImageItems = list
    }
    //image states
    var selectedImages = remember { mutableStateListOf<ImageItem>() }
    var selectAll by remember { mutableStateOf(false) }
    var imageSelectionMode by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var deleteImages by remember { mutableStateOf(false) }


    //update menu status
    val anyImageSelected = selectedImages.isNotEmpty()
    val showMenuItems = totalImagesCount > 0
    //modal bottom sheet
    val sheetState = rememberModalBottomSheetState()
    //Request permission
    var requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted
        } else {
            // Permission is denied
        }
    }

    fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End

                        ) {
                            if (showMenuItems) {
                                var expanded by remember { mutableStateOf(false) }
                                ActionButtonWithText(
                                    enabled = true,
                                    onClick = {
                                        selectAll = !selectAll
                                        imageSelectionMode = true
                                        if (selectAll) {
                                            selectedImages.addAll(actualImageItems.filterNotNull())
                                        } else {
                                            selectedImages.clear()
                                        }
                                    },
                                    iconId = R.drawable.ic_compress_24dp,
                                    modifier = Modifier.padding(end = 8.dp),
                                    text = stringResource(R.string.select_all)
                                )

                                ActionButtonWithText(
                                    enabled = anyImageSelected,
                                    onClick = {
                                        showShareSheet = true
                                    },
                                    iconId = R.drawable.ic_compress_24dp,
                                    modifier = Modifier.padding(end = 8.dp),
                                    text = stringResource(R.string.share)
                                )


                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = "Localized description"
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.delete)) },
                                            onClick = {
                                                showDeleteConfirmationDialog = true
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = stringResource(R.string.delete)
                                                )
                                            },
                                            enabled = anyImageSelected
                                        )

                                    }
                                }

                            }
                        }
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
            )
        }
    ) { innerPadding ->

        LaunchedEffect(key1 = Unit) {
            requestStoragePermission()
        }
        if (showDeleteConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmationDialog = false },
                title = { Text(stringResource(R.string.delete_confirmation_title)) },
                text = { Text(stringResource(R.string.delete_confirmation_message)) },
                confirmButton = {
                    Button(onClick = {
                        deleteImages= true


                        showDeleteConfirmationDialog = false
                    }) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        if(deleteImages) {

                deleteSelectedImages(context, selectedImages.map { it.uri }, onDeleted = {
                    val list = actualImageItems.toMutableList()
                    selectedImages.forEach {
                        list.remove(it)
                    }
                    actualImageItems = list
                    selectedImages.clear()
                    selectAll = false
                    imageSelectionMode = false
                    actualImageItems = getActualImageUris(context, placeholders)
                    deleteImages = false
                })

        }
        if (showShareSheet) {
            ModalBottomSheet(
                onDismissRequest = { showShareSheet = false },
                sheetState = sheetState,
                content = {
                    ShareBottomSheet(selectedImages.map { it.uri }, context) {
                        showShareSheet = false
                    }
                }
            )
        }

        if (actualImageItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No compressed images found.")
            }
        } else {
            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                itemsIndexed(
                    items = actualImageItems,
                    key = { index, imageItem -> imageItem.uri },
                ) { index, imageItem ->
                    AnimatedVisibility(
                        visible = true,
                        exit = fadeOut(animationSpec = tween(500))
                    ) {
                        val isSelected = selectedImages.contains(imageItem)
                        ImageCard(imageItem, false, isSelected) {
                            imageSelectionMode = true
                            if (isSelected) {
                                selectedImages.remove(imageItem)
                            } else {
                                selectedImages.add(imageItem)
                            }
                            selectAll =
                                selectedImages.size == actualImageItems.filterNotNull().size
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun ImageCard(
    imageItem: ImageItem,
    isLoading: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            imageItem.fileSize?.let {
                val fileSizeInKb = it / 1024
                val fileSizeText = if (fileSizeInKb > 1000) {
                    "${fileSizeInKb / 1024} mb"
                } else "$fileSizeInKb kb"
                Text(fileSizeText, maxLines = 1)
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
            SubcomposeAsyncImage(
                model = imageItem.uri,
                contentDescription = "Compressed Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Crop,
            ) {
                val state = painter.state
                if (isLoading || state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                    val shimmerBrush = ShimmerEffect()
                    Box(modifier = Modifier.background(shimmerBrush))
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
        }

    }
}

@Composable
fun ShareBottomSheet(selectedImages: List<Uri>, context: Context, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.share_images_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            val uris = ArrayList<Uri>()
            selectedImages.forEach {
                uris.add(it)
            }
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }

        val shareIntent = Intent.createChooser(intent, stringResource(R.string.share_images))
        context.startActivity(shareIntent)
        onDismiss()
    }
}

fun getActualImageUris(context: Context, placeholders: List<Nothing?>): List<ImageItem> {
    val imageItems = mutableListOf<ImageItem>()

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE
    )
    val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("imageResizer_%")
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(
        queryUri,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )

    cursor?.use {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val totalCount = cursor.count
        if (totalCount == 0) {
            return emptyList()
        }

        for (i in 0 until min(placeholders.size, totalCount)) {
            if (cursor.moveToPosition(i)) {
                val id = cursor.getLong(idColumn)
                val imageName = cursor.getString(nameColumn)
                val fileSize = cursor.getLong(sizeColumn)
                val contentUri = ContentUris.withAppendedId(queryUri, id)
                val imageDimensions = imageDimensionsFromUri(context, contentUri)
                imageItems.add(
                    ImageItem(
                        uri = contentUri,
                        imageName = imageName,
                        fileSize = fileSize,
                        originalBitmap = null,
                        scaledBitmap = null,
                        imageDimension = imageDimensions
                    )
                )
            }
        }

        // Add null items for the remaining placeholders
        val remaining = placeholders.size - totalCount
        for (i in 0 until remaining) {
            imageItems.add(
                ImageItem(
                    uri = Uri.EMPTY,
                    imageName = null,
                    fileSize = null,
                    originalBitmap = null,
                    scaledBitmap = null
                )
            )
        }
    }

    return imageItems
}




fun getTotalTransformedImagesCount(context: Context): Int {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE
    )
    val customDirectoryName: String = "ImageResizer"
    val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
//    val selectionArgs = arrayOf("%$customDirectoryName/%")
    val selectionArgs = arrayOf("imageResizer_%")

    val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val contentResolver = context.contentResolver
    var imageCount = 0
    val cursor = contentResolver.query(
        queryUri,
        projection,
        selection,
        selectionArgs,
        null
    )
    cursor?.use {
        imageCount = cursor.count
    }
    return imageCount
}
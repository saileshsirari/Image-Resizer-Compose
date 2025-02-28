package com.image.resizer.compose

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    val totalImagesCount = getTotalCompressedImagesCount(context)

    var loadingImages by remember {
        mutableStateOf(mutableSetOf<Int>())
    }
    val placeholders = remember {
        List(totalImagesCount) { null }
    }
    var actualImageUris by remember {
        mutableStateOf<List<Uri?>>(emptyList())
    }
    val lazyGridState = rememberLazyGridState()
    LaunchedEffect(true) {
        val newImages = mutableSetOf<Int>()
        for (i in 1..totalImagesCount) {
            newImages.add(i)
        }
        loadingImages.addAll(newImages)
        val urisForPage = withContext(Dispatchers.IO) {
            getRealCompressedImageUris(context, newImages.toList())
        }
        val list = actualImageUris.toMutableList()
        list.addAll(urisForPage)
        actualImageUris = list
    }
    //image states
    var selectedImages = remember { mutableStateListOf<Uri>() }
    var selectAll by remember { mutableStateOf(false) }
    var imageSelectionMode by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
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
            } else {
                val list = actualImageUris.toMutableList()
                selectedImages.forEach {
                    list.remove(it)
                }
                actualImageUris = list
                selectedImages.clear()
                selectAll = false
                imageSelectionMode = false
                actualImageUris = getActualImageUris(context, placeholders)
            }

        } else {
            // Handle failure or user cancellation
            Log.e("MyImagesScreen", "Deletion failed or cancelled by user")
        }
        deletePendingUris = emptyList()
        deletePendingRecoverableSecurityException = null
    }
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
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Row(
                            modifier = Modifier.then(
                                Modifier
                                    .then(Modifier.requiredWidthIn(min = 1.dp))
                                    .width(IntrinsicSize.Max)
                            )
                                .wrapContentWidth()
                                .fillMaxHeight()
                                .padding(end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End

                        ){
                            if (showMenuItems) {
                                var expanded by remember { mutableStateOf(false) }
                                ActionButtonWithText(
                                    enabled = true ,
                                    onClick = {
                                        selectAll = !selectAll
                                        imageSelectionMode = true
                                        if (selectAll) {
                                            selectedImages.addAll(actualImageUris.filterNotNull())
                                        } else {
                                            selectedImages.clear()
                                        }
                                    },
                                    iconId = R.drawable.ic_compress_24dp,
                                    modifier=Modifier.padding(end = 8.dp),
                                    text = stringResource(R.string.select_all)
                                )

                                ActionButtonWithText(
                                    enabled = anyImageSelected ,
                                    onClick = {
                                        showShareSheet = true
                                    },
                                    iconId = R.drawable.ic_compress_24dp,
                                    modifier=Modifier.padding(end = 8.dp),
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
                        deleteSelectedImages(context, selectedImages, launcher)

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
        if (showShareSheet) {
            ModalBottomSheet(
                onDismissRequest = { showShareSheet = false },
                sheetState = sheetState,
                content = {
                    ShareBottomSheet(selectedImages, context) {
                        showShareSheet = false
                    }
                }
            )
        }

        if (actualImageUris.isEmpty()) {
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
                    items = actualImageUris,
                    key = { index, uri -> uri?.hashCode() ?: index }
                ) { index, uri ->
                    AnimatedVisibility(
                        visible = uri != null,
                        exit = fadeOut(animationSpec = tween(500))
                    ) {
                        if (uri != null) {
                            val isSelected = selectedImages.contains(uri)
                            ImageCard(uri, false, isSelected, imageSelectionMode) {
                                imageSelectionMode = true
                                if (isSelected) {
                                    selectedImages.remove(uri)
                                } else {
                                    selectedImages.add(uri)
                                }
                                selectAll =
                                    selectedImages.size == actualImageUris.filterNotNull().size
                            }
                        } else {
                            ImageCard(
                                uri,
                                loadingImages.contains(index),
                                false,
                                imageSelectionMode
                            ) {}
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun ImageCard(
    uri: Uri?,
    isLoading: Boolean,
    isSelected: Boolean,
    imageSelectionMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box {
            SubcomposeAsyncImage(
                model = uri,
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
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
            )
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

fun getActualImageUris(context: Context, placeholders: List<Nothing?>): List<Uri?> {
    val totalImagesCount = getTotalCompressedImagesCount(context)
    val imageUris = mutableListOf<Uri?>()

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
    )
    val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("compressed_image_%")
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
        val totalCount = cursor.count
        if (totalCount == 0) {
            return emptyList()
        }
        for (i in 0 until min(placeholders.size, totalCount)) {
            if (cursor.moveToPosition(i)) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(queryUri, id)
                imageUris.add(contentUri)
            }
        }

        val remaining = placeholders.size - totalCount
        for (i in 0 until remaining) {
            imageUris.add(null)
        }
    }

    return imageUris
}

fun deleteSelectedImages(
    context: Context,
    selectedImages: List<Uri>,
    launcher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>
) {
    val urisToDelete = mutableListOf<Uri>()
    val contentResolver = context.contentResolver

    val recoverableSecurityExceptions = mutableListOf<RecoverableSecurityException>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val trashIntent = MediaStore.createTrashRequest(
            contentResolver,
            selectedImages, true
        )
        val intentSenderRequest = IntentSenderRequest.Builder(trashIntent).build()
        launcher.launch(intentSenderRequest)

    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

        selectedImages.forEach { uri ->
            try {
                contentResolver.delete(uri, null, null)
            } catch (securityException: SecurityException) {
                val recoverableSecurityException =
                    securityException as? RecoverableSecurityException
                if (recoverableSecurityException != null) {
                    recoverableSecurityExceptions.add(recoverableSecurityException)
                    urisToDelete.add(uri)
                }

            }
        }
        if (recoverableSecurityExceptions.isNotEmpty()) {
            val intentSender =
                recoverableSecurityExceptions.first().userAction.actionIntent.intentSender
            val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
            launcher.launch(intentSenderRequest)
        }

    } else {
        selectedImages.forEach { uri ->
            try {
                contentResolver.delete(uri, null, null)
            } catch (securityException: SecurityException) {
                securityException.printStackTrace()
                // Handle the exception for pre-Q devices
                // You can show a message to the user indicating that deletion failed.
                // Example: Toast.makeText(context, "Deletion failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun getRealCompressedImageUris(context: Context, indexesToLoad: List<Int>): List<Uri?> {
    val compressedImageUris = mutableListOf<Uri?>()
    if (indexesToLoad.isEmpty()) return compressedImageUris

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE
    )
    val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("compressed_image_%")
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
        val imageIds = MutableList<Long?>(indexesToLoad.size) { null }
        val totalCount = cursor.count
        if (totalCount == 0) {
            return emptyList()
        }
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val contentUri = ContentUris.withAppendedId(queryUri, id)
            compressedImageUris.add(contentUri)
        }
    }
    return compressedImageUris
}

fun getTotalCompressedImagesCount(context: Context): Int {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE
    )
    val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("compressed_image_%")
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
package com.image.resizer.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import apps.sai.com.imageresizer.R
import coil.request.ImageRequest
import coil.size.Size
import com.image.resizer.compose.mediaApi.MediaHandleUseCase
import com.image.resizer.compose.mediaApi.MediaRepositoryImpl
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch


@Preview
@Composable
fun GalleryImagesComponentPreview1() {
    val uri = "content://media/external/file/25".toUri()
    val context = LocalContext.current
    val mediaRepository = MediaRepositoryImpl(LocalContext.current)
    val mediaHandleUseCase = MediaHandleUseCase(mediaRepository)
    val homeScreenViewModel = HomeScreenViewModel(mediaHandleUseCase)
    val imageItems = listOf(
        ImageItem(
            context,
            uri = uri,
        ),
        ImageItem(
            context,
            uri = uri,
        ),
        ImageItem(
            context = context,
            uri = uri,
        ),
    )
    // GalleryImagesComponent(homeScreenViewModel.selectedImageItems)
}


@Composable
fun GalleryImagesComponent(
    selectedImageItems: List<ImageItem>, onImageItemClicked: (ImageItem) -> Unit = {}
) {
    val imageItems = selectedImageItems
    val columns = if (imageItems.size > 1) {
        GridCells.Fixed(2)
    } else {
        GridCells.Fixed(1)
    }
    Spacer(modifier = Modifier.height(10.dp))
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = columns,
        contentPadding = PaddingValues(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(imageItems) { imageItem ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {

                imageItem.imageDimension.let {
                    Text(" ${it.first}x${it.second}")
                }
                imageItem.fileSize.let {
                    Text("${it / 1024} kb", maxLines = 1)
                }

                Spacer(modifier = Modifier.height(4.dp))


                AsyncImage(
                    placeholder = painterResource(R.drawable.ic_undo_24dp),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageItem.uri)
                        .scale(coil.size.Scale.FIT)
                        .size(Size(300, 300))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.Companion
                        .align(Alignment.CenterHorizontally)
                        .padding(4.dp)
                        .height(200.dp)
                        .clickable {
                            onImageItemClicked(imageItem)
                            // val uri =   compressImageToTargetSize(context ,imageItem,100).computedUri.toString()

                        }
                )
            }
        }
    }
}

@Composable
internal fun ScaledImagesGrid(
    modifier: Modifier,
    imageItems: List<ImageItem>,
    homeScreenViewModel: HomeScreenViewModel,
    onSelectedItemClicked: (ImageItem) -> Unit = {}
) {
    val scaledImages by homeScreenViewModel.scaledImageItems.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lazyGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    //1
    val visibleItems = remember {
        derivedStateOf {
            lazyGridState.layoutInfo.visibleItemsInfo.map { it.index }
        }
    }

    LaunchedEffect(lazyGridState) {
        //2
        snapshotFlow { visibleItems.value }.filter { it.isNotEmpty() }.collectLatest { indices ->
            //3
            indices.forEach { index ->
                if (index < scaledImages.size) {
                    val imageItem = scaledImages[index]
                    //4
                    scope.launch(Dispatchers.IO) {
                        //5
                        imageItem.computeScaledUri()
                    }
                }
            }
        }
    }

    if (imageItems.isEmpty()) {
        Text(
            text = "No images selected.",
            textAlign = TextAlign.Center
        )
    } else {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(
                bottom = 5.dp,
                top = 10.dp,
                start = 5.dp,
                end = 5.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = modifier
        ) {
            items(scaledImages, key = { item -> item.key.toString() }) { imageItem ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.Gray),
                    horizontalArrangement = Arrangement.SpaceBetween, // Center columns
                    verticalAlignment = Alignment.CenterVertically // Center vertically
                ) {
                    // First Column
                    Column(
                        modifier = Modifier
                            .weight(1f) // Equal weight for both columns
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, // Center image
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        imageItem.imageDimension.let {
                            Text("Original : ${it.first}x${it.second}")
                        }
                        imageItem.fileSize.let {
                            val fileSizeInKb = it / 1024
                            val fileSizeText = if (fileSizeInKb > 1000) {
                                "${fileSizeInKb / 1024} mb"
                            } else "$fileSizeInKb kb"
                            Text(fileSizeText, maxLines = 1)
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageItem.uri)
                                .scale(coil.size.Scale.FIT)
                                .size(Size(300, 300))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Original Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.Companion
                                .align(Alignment.CenterHorizontally)
                                .padding(4.dp)
                                .height(300.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .clickable(onClick = {
                                    onSelectedItemClicked(imageItem)
                                }),
                            placeholder = painterResource(id = R.drawable.ic_crop_24dp),
                            error = painterResource(id = R.drawable.ic_crop_24dp)

                        )


                    }


                    // Second Column
                    Column(
                        modifier = Modifier
                            .weight(1f) // Equal weight for both columns
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, // Center image
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        imageItem.scaledUri?.let {
                            if (imageItem.scaledImageDimension != null) {
                                Text("Scaled : ${imageItem.scaledImageDimension?.first ?: 0}x${imageItem.scaledImageDimension?.second ?: 0}")
                                imageItem.scaledFileSize?.let {
                                    val fileSizeInKb = it / 1024
                                    val fileSizeText = if (fileSizeInKb > 1000) {
                                        "${fileSizeInKb / 1024} mb"
                                    } else "$fileSizeInKb kb"
                                    Text(fileSizeText, maxLines = 1)
                                }
                            }
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageItem.scaledUri)
                                .size(Size(300, 300))
                                .scale(coil.size.Scale.FIT)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Scaled Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.Companion
                                .align(Alignment.CenterHorizontally)
                                .padding(4.dp)
                                .height(300.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .clickable(onClick = {
                                    onSelectedItemClicked(imageItem)
                                }),
                            placeholder = painterResource(id = R.drawable.ic_crop_24dp),
                            error = painterResource(id = R.drawable.ic_crop_24dp)
                        )

                    }
                }
            }
        }
    }
}


internal fun imageDimensionsFromUri(
    context: Context,
    uri: Uri
): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
    val originalWidth = options.outWidth
    val originalHeight = options.outHeight
    return Pair(originalWidth, originalHeight)
}


@Composable
fun ImagePreview(imageUri: Uri?) {
    var bitmap: Bitmap? by remember { mutableStateOf(null) }
    val context = LocalContext.current

    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            bitmap = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(imageUri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Selected Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Fit
        )
    }
}
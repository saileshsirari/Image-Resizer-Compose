package com.image.resizer.compose

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.request.ImageRequest
import coil.size.Size
import com.image.resizer.compose.mediaApi.MediaHandleUseCase
import com.image.resizer.compose.mediaApi.MediaRepositoryImpl
import com.image.resizer.compose.mediaApi.loadBitmapFromUri
import kotlinx.coroutines.flow.StateFlow


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
    GalleryImagesComponent(homeScreenViewModel.selectedImageItems)
}


@Composable
fun GalleryImagesComponent(selectedImageItems: StateFlow<List<ImageItem>>) {
    val imageItems by selectedImageItems.collectAsStateWithLifecycle()
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

                imageItem.imageDimension?.let {
                    Text(" ${it.first}x${it.second}")
                }
                imageItem.fileSize?.let {
                    Text("${it / 1024} kb", maxLines = 1)
                }

                Spacer(modifier = Modifier.height(4.dp))


                AsyncImage(
                    placeholder = painterResource(R.drawable.ic_undo_24dp),
                    model  = ImageRequest.Builder(LocalContext.current)
                        .data(imageItem.uri)
                        .scale(coil.size.Scale.FIT)
                        .size(Size(300,300))
                        .crossfade(true)
                        .diskCacheKey(imageItem.fileSize.toString()+ imageItem.imageDimension?.first)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.Companion
                        .align(Alignment.CenterHorizontally)
                        .padding(4.dp)
                        .height(200.dp)
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
            items(scaledImages, key = { item -> item.uri.hashCode() }) { imageItem ->
                Row(
                    modifier = Modifier
                        .clickable(onClick = {
                            loadBitmapFromUri(imageItem.uri,context)?.also {
                                val item = imageItem.saveBitmapToTempAndGetUri(context, it)
                                onSelectedItemClicked(item)
                            }

                        })
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
                            Text("Original : ${it.first }x${it.second }")
                        }
                        imageItem.fileSize.let {
                            val fileSizeInKb = it / 1024
                            val fileSizeText = if (fileSizeInKb > 1000) {
                                "${fileSizeInKb / 1024} mb"
                            } else "$fileSizeInKb kb"
                            Text(fileSizeText, maxLines = 1)
                        }
                        AsyncImage(
                            model = imageItem.uri,
                            contentDescription = "Original Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.Companion
                                .align(Alignment.CenterHorizontally)
                                .fillMaxSize()
                                .padding(4.dp)
                                .height(300.dp)
                                .clip(RoundedCornerShape(1.dp))
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
                        if (imageItem.scaledUri != null) {
                            AsyncImage(
                                model = imageItem.scaledUri,
                                contentDescription = "Scaled Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.Companion
                                    .align(Alignment.CenterHorizontally)
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(1.dp))
                            )
                        } else {
                            Text(
                                text = "loading ...",
                                textAlign = TextAlign.Center
                            )
                        }
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

internal fun saveImagesToGallery(
    context: Context, imageItems: List<ImageItem?>,
    customDirectoryName: String = "ImageResizer"
) {
    val customDirectoryName = customDirectoryName
    val resolver = context.contentResolver

    return runCatching {

        imageItems.filterNotNull().forEach { imageItem ->
            imageItem.scaledUri?.let { it ->
                loadBitmapFromUri(it,context)?.let { bitmap->
                    val displayName =
                        "imageResizer_${
                            SimpleDateFormat(
                                "MMdd_HHmmss",
                                Locale.getDefault()
                            ).format(Date())
                        }.jpg"
                    val mimeType = "image/jpeg"

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // For Android 10 (Q) and above
                        val values = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                            put(
                                MediaStore.MediaColumns.RELATIVE_PATH,
                                "${Environment.DIRECTORY_PICTURES}/$customDirectoryName"
                            )
                        }

                        try {
                            val uri =
                                resolver.insert(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    values
                                )
                            uri?.let {
                                resolver.openOutputStream(uri)?.use { outputStream ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                }
                            }
                            Log.d("SaveImage", "Image saved to gallery (Q+): $uri")

                        } catch (e: IOException) {
                            Log.e("SaveImage", "Error saving image (Q+): ${e.message}")
                        }
                    } else {
                        // For Android versions before 10 (Q)
                        val picturesDir =
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        val customDir = File(picturesDir, customDirectoryName)
                        if (!customDir.exists()) {
                            customDir.mkdirs()
                        }
                        val file = File(customDir, displayName)
                        try {
                            FileOutputStream(file).use { outputStream ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                            }
                            // Make sure the file is visible in the gallery immediately
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(file.toString()),
                                arrayOf(mimeType),
                                null
                            )
                            Log.d("SaveImage", "Image saved to gallery (pre-Q): $file")
                        } catch (e: IOException) {
                            Log.e("SaveImage", "Error saving image (pre-Q): ${e.message}")
                        }
                    }
                }
            }
        }
    }.getOrElse {
        throw it
    }
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
package com.image.resizer.compose

import android.R.attr.bottom
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.scale
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import kotlin.io.path.exists
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap


data class ImageItem(
    val uri: Uri,
    var scaledBitmap: Bitmap? = null,
    var originalBitmap: Bitmap? = null
)


@Preview
@Composable
fun ScaledImageScreenPreview() {
    val imageItems = listOf(
        ImageItem(
            uri = "content://media/external/file/54".toUri(),
            scaledBitmap = createBitmap(200, 200),
            originalBitmap = createBitmap(100, 100)
        ),
        ImageItem(
            uri = "content://media/external/file/54".toUri(), scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ),
        ImageItem(
            uri = "content://media/external/file/54".toUri(), scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ),
        ImageItem(
            uri = "content://media/external/file/54".toUri(), scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ),
        ImageItem(
            uri = "content://media/external/file/54".toUri(), scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ), ImageItem(
            uri = "content://media/external/file/54".toUri(), scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        ),
        ImageItem(
            uri = "content://media/external/file/54".toUri(), scaledBitmap = null,
            originalBitmap = createBitmap(100, 100)
        )
    )
    val scaleParams = imageItems.map {
        ScaleParams(
            newHeight = 100,
            newWidth = 100,
        )
    }

    ScaledImageScreen(
        imageItems = imageItems,
        scaleParamsList = scaleParams,
        onSaveClicked = {
            println("Save button clicked")
        }
    )
}

@Composable
fun ScaledImageScreen(
    imageItems: List<ImageItem>,
    scaleParamsList: List<ScaleParams>,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var imagesScaled by remember { mutableStateOf(false) }
    var scaledImages by remember { mutableStateOf(listOf<ImageItem>()) }

    val context = LocalContext.current

    LaunchedEffect(scaleParamsList) {
        if (scaleParamsList.isNotEmpty()) {
            imagesScaled = false
            withContext(Dispatchers.IO) {
                scaleImages(imageItems, scaleParamsList, context) {
                    imagesScaled = true
                    scaledImages = imageItems
                }

            }
        }
    }
    Column(
        modifier = Modifier.Companion
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Companion.CenterHorizontally
    ) {
        if (imageItems.isEmpty()) {
            Text(
                text = "No images selected.",
                textAlign = TextAlign.Center
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(bottom = 20.dp, top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = modifier
                    .border(10.dp, Color.Yellow, RoundedCornerShape(8.dp))
                    .weight(5f)
            ) {
                items(scaledImages.size) { index ->
                    val imageItem = scaledImages[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Red),
                        horizontalArrangement = Arrangement.Center, // Center columns
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
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (imageItem.originalBitmap != null) {
                                Text("Original : ${imageItem.originalBitmap?.width ?: 0}x${imageItem.originalBitmap?.height ?: 0}")

                                Image(
                                    bitmap = imageItem.originalBitmap!!.asImageBitmap(),
                                    contentDescription = "Scaled Image",
                                    modifier = Modifier
                                        .sizeIn(
                                            minWidth = 200.dp,
                                            minHeight = 200.dp,
                                            maxHeight = 300.dp
                                        )
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .padding(1.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = "Scaling...",
                                    textAlign = TextAlign.Center
                                )
                            }
                        }


                        // Second Column
                        Column(
                            modifier = Modifier
                                .weight(1f) // Equal weight for both columns
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, // Center image
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (imageItem.scaledBitmap != null) {
                                Text("Scaled: ${imageItem.scaledBitmap?.width ?: 0}x${imageItem.scaledBitmap?.height ?: 0}")
                                Image(
                                    bitmap = imageItem.scaledBitmap!!.asImageBitmap(),
                                    contentDescription = "Scaled",
                                    modifier = Modifier
                                        .sizeIn(
                                            minWidth = 200.dp,
                                            minHeight = 200.dp,
                                            maxHeight = 300.dp
                                        )
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .padding(1.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (imageItems.size > 1) {
                                Text(
                                    text = "Scaling...",
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

        }

        if (imagesScaled) {
            Column(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = {
                        onSaveClicked()
                        saveImagesToGallery(context, imageItems)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Save Scaled Images")
                }
            }

        }
    }
}


private suspend fun scaleImages(
    imageItems: List<ImageItem>,
    scaleParamsList: List<ScaleParams>,
    context: Context,
    onComplete: () -> Unit
) {
    imageItems.forEachIndexed { index, imageItem ->
        val scaleParams = scaleParamsList[index]

        imageItem.originalBitmap = context.contentResolver.openInputStream(imageItem.uri)?.use {
            BitmapFactory.decodeStream(it)
        }

        val scaledWidth =
            scaleParams.newWidth
        val scaledHeight =
            scaleParams.newHeight

        val scaledBitmap = context.contentResolver.openInputStream(imageItem.uri)?.use {
            BitmapFactory.decodeStream(it)?.scale(scaledWidth, scaledHeight, false)
        }
        imageItem.scaledBitmap = scaledBitmap
    }
    onComplete()
}

internal fun imageDimensionsFromUri(
    context: Context,
    imageItem: ImageItem
): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    context.contentResolver.openInputStream(imageItem.uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
    val originalWidth = options.outWidth
    val originalHeight = options.outHeight
    return Pair(originalWidth, originalHeight)
}

private fun saveImagesToGallery(context: Context, imageItems: List<ImageItem>) {
    val customDirectoryName = "ScaledImages"
    val resolver = context.contentResolver

    imageItems.forEach { imageItem ->
        imageItem.scaledBitmap?.let { bitmap ->
            val displayName =
                "Scaled_${
                    SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
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
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
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
package com.image.resizer.compose

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScaledImagesGrid(imageItems: List<ImageItem>) {
    val context = LocalContext.current
    var selectedImageItem by remember { mutableStateOf<ImageItem?>(null) }
    val lazyGridState = rememberLazyGridState()
    var showComparisonView by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                showComparisonView = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    if (!showComparisonView) {
        Scaffold(
        ) { innerPadding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = lazyGridState,
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(imageItems, key = { item -> item.uri.hashCode() }) { imageItem ->
                    ImageItemCard(imageItem = imageItem) {
                        selectedImageItem = saveBitmapToTempAndGetUri(context, imageItem)
                        showComparisonView = true

                    }
                }
            }
        }

    }
    if (showComparisonView && selectedImageItem != null) {

        AnimatedVisibility(
            visible = showComparisonView,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 500)
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 500)
            ) + fadeOut()
        ) {
            ImageComparisonView(imageItem = selectedImageItem!!)
        }
    }
}


@Composable
fun ImageItemCard(imageItem: ImageItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = imageItem.imageName ?: "No Name",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Box {
                AsyncImage(
                    model = imageItem.uri,
                    contentDescription = "Compressed Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

fun saveBitmapToTempAndGetUri(context: Context, imageItem: ImageItem): ImageItem {
    if (imageItem.scaledBitmap == null) {
        return imageItem
    }
    val bitmap = imageItem.scaledBitmap!!

    val file = File(context.cacheDir, "scaled_image_${System.currentTimeMillis()}.jpg")
    try {
        file.createNewFile()
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        val bitmapData = bos.toByteArray()

        val fos = FileOutputStream(file)
        fos.write(bitmapData)
        fos.flush()
        fos.close()

        // Create URI
        val tempUri = Uri.fromFile(file)

        // Update scaledUri in ImageItem
        val updatedImageItem = imageItem.copy(scaledUri = tempUri,
        )

        val fileSize = file.length()
        val imageDimension = imageDimensionsFromUri(context, tempUri)
        return updatedImageItem.copy(
            scaledFileSize = fileSize,
            scaledImageDimension = imageDimension
        )
    } catch (e: IOException) {
        Log.e("saveBitmapToTempAndGetUri", "Error saving bitmap to temp file: ${e.message}")
        e.printStackTrace()
    }
    return imageItem
}
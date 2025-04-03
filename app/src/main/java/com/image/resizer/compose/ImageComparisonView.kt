package com.image.resizer.compose

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri

@Composable
fun ImageComparisonView(imageItem: ImageItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ComparisonImageView(
            label = "Original Image",
            uri = imageItem.uri,
            fileSize = imageItem.fileSize,
            dimensions = imageItem.imageDimension
        )
        Spacer(modifier = Modifier.height(16.dp))
        ComparisonImageView(
            label = "Scaled Image",
            uri = imageItem.scaledUri ?: Uri.EMPTY,
            fileSize = imageItem.scaledFileSize,
            dimensions = imageItem.scaledImageDimension
        )
    }
}

@Composable
fun ComparisonImageView(
    label: String,
    uri: Uri,
    fileSize: Long?,
    dimensions: Pair<Int, Int>?
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }
    val context = LocalContext.current
    val bitmap = getBitmapFromUri(uri, context)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Row {
            dimensions?.let {
                Text(
                    "Dimensions: ${it.first}x${it.second}",
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            fileSize?.let {
                val fileSizeInKb = it / 1024
                val fileSizeText = if (fileSizeInKb > 1000) {
                    "${fileSizeInKb / 1024} mb"
                } else "$fileSizeInKb kb"
                Text("Size: $fileSizeText")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .transformable(state)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        ) {
          if (uri != Uri.EMPTY) {
                AsyncImage(
                    model = uri,
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                )
            } else {
                Text(text = "loading...", textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
@Preview
fun MyScreen() {
    val context = LocalContext.current
    val uri = "content://media/external/file/25".toUri()
    val imageItem =  ImageItem(
        context =context,
        uri = uri,
        scaledImageDimension = Pair(100,200),
        scaledFileSize = 512,
    )
    ImageComparisonView(imageItem = imageItem)
}

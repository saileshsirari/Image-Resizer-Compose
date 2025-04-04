package com.image.resizer.compose

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.util.Log.e
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


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

fun ImageItem.saveBitmapToTempAndGetUri(context: Context, bitmap: Bitmap): ImageItem {

    val file = File(context.cacheDir, "$imageName")
    if(!file.exists()) {
        file.createNewFile()
    }
    val bos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
    val bitmapData = bos.toByteArray()

    val fos = FileOutputStream(file,false)
    fos.write(bitmapData)
    fos.flush()
    fos.close()

    // Create URI
    val tempUri = Uri.fromFile(file)

    // Update scaledUri in ImageItem
    val updatedImageItem = this.copy(
        computedUri = tempUri,
    )

    val fileSize = file.length()
    val imageDimension = imageDimensionsFromUri(context, tempUri)
    return updatedImageItem.copy(
        computedUri = tempUri,
        scaledFileSize = fileSize,
        scaledImageDimension = imageDimension
    )
    return this
}
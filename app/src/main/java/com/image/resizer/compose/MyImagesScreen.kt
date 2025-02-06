package com.image.resizer.compose

import android.net.Uri
import androidx.compose.animation.core.copy
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun MyImagesScreen() {
    val context = LocalContext.current
    var paginationState by remember {
        mutableStateOf(PaginationState())
    }
    var compressedImageUris by remember {
        mutableStateOf(emptyList<Uri>())
    }

    val totalImagesCount = getTotalCompressedImagesCount(context)
    val totalPages = (totalImagesCount + IMAGES_PER_PAGE - 1) / IMAGES_PER_PAGE
    if (paginationState.totalPages != totalPages)
        paginationState = paginationState.copy(totalPages = totalPages)
    LaunchedEffect(key1 = paginationState.currentPage) {
        paginationState = paginationState.copy(isLoading = true)
        val uris = getCompressedImageUris(context, paginationState.currentPage, IMAGES_PER_PAGE)
        compressedImageUris = compressedImageUris + uris
        paginationState =
            paginationState.copy(isLoading = false, isLastPage = uris.size < IMAGES_PER_PAGE)
    }
    if (compressedImageUris.isEmpty() && !paginationState.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No compressed images found.")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items (compressedImageUris) { uri ->
                ImageCard(uri)
            }
            if (!paginationState.isLastPage && !paginationState.isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                if (!paginationState.isLoading)
                                    paginationState =
                                        paginationState.copy(currentPage = paginationState.currentPage + 1)
                            },
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            item {
                Text(
                    text = "Page ${paginationState.currentPage} of ${paginationState.totalPages}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ImageCard(uri: Uri) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Compressed Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentScale = ContentScale.Crop
        )
    }
}
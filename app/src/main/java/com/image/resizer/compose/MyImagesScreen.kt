package com.image.resizer.compose

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.indices
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.min

data class PaginationState(
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
)

const val IMAGES_PER_PAGE = 30
const val MAX_IMAGE_SIZE_ALLOWED_IN_BYTES = 1024 * 1024 * 2 // 2MB

@Composable
fun MyImagesScreen() {
    val context = LocalContext.current
    var paginationState by remember {
        mutableStateOf(PaginationState())
    }
    val totalImagesCount = getTotalCompressedImagesCount(context)
    val placeholders = remember {
        List(totalImagesCount) { null }
    }
    var actualImageUris by remember {
        mutableStateOf<List<Uri?>>(placeholders)
    }
    var loadingImages by remember {
        mutableStateOf(mutableSetOf<Int>())
    }

    var loadingNextPage by remember {
        mutableStateOf(false)
    }

    val totalPages = (totalImagesCount + IMAGES_PER_PAGE - 1) / IMAGES_PER_PAGE
    if (paginationState.totalPages != totalPages)
        paginationState = paginationState.copy(totalPages = totalPages)
    val lazyGridState = rememberLazyGridState()

    LaunchedEffect(key1 = paginationState.currentPage) {
        if (paginationState.currentPage > 1) {
            loadingNextPage = true
            delay(1000)
            loadingNextPage = false
        }
        paginationState = paginationState.copy(isLoading = true)
        val startIndex = (paginationState.currentPage - 1) * IMAGES_PER_PAGE
        val endIndex = minOf(startIndex + IMAGES_PER_PAGE, placeholders.size)
        val newImages = mutableSetOf<Int>()
        for (i in startIndex until endIndex) {
            newImages.add(i)
        }
        loadingImages.addAll(newImages)
        val urisForPage = withContext(Dispatchers.IO) {
            getRealCompressedImageUris(context, newImages.toList())
        }

        // Stage the changes here
        val updatedActualImageUris = actualImageUris.toMutableList()
        val updatedLoadingImages = loadingImages.toMutableSet()

        for (i in urisForPage.indices) {
            val uri = urisForPage[i]
            if (uri != null) {
                val index = newImages.toList()[i]
                updatedActualImageUris[index] = uri
                updatedLoadingImages.remove(index)
            }
        }

        // Apply the changes after the loop
        actualImageUris = updatedActualImageUris
        loadingImages.clear()
        loadingImages.addAll(updatedLoadingImages)
        paginationState = paginationState.copy(isLoading = false)
    }
    LaunchedEffect(lazyGridState.layoutInfo.visibleItemsInfo) {
        val visibleIndexes = lazyGridState.layoutInfo.visibleItemsInfo.map { it.index }
        val indexesToLoad = mutableSetOf<Int>()
        for (index in visibleIndexes) {
            if (index < actualImageUris.size && actualImageUris[index] == null && !loadingImages.contains(index)) {
                indexesToLoad.add(index)
            }
        }
        if (indexesToLoad.isNotEmpty()) {
            val newLoadingImages = loadingImages.toMutableSet()
            newLoadingImages.addAll(indexesToLoad)
            loadingImages.clear()
            loadingImages.addAll(newLoadingImages)

            val urisForPage = withContext(Dispatchers.IO) {
                getRealCompressedImageUris(context, indexesToLoad.toList())
            }
            val updatedActualImageUris = actualImageUris.toMutableList()
            val updatedLoadingImages = loadingImages.toMutableSet()
            for (i in urisForPage.indices) {
                val uri = urisForPage[i]
                if (uri != null) {
                    val index = indexesToLoad.toList()[i]
                    updatedActualImageUris[index] = uri
                    updatedLoadingImages.remove(index)
                }
            }
            actualImageUris = updatedActualImageUris
            loadingImages.clear()
            loadingImages.addAll(updatedLoadingImages)
        }
    }
    if (placeholders.isEmpty() && !paginationState.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(placeholders) { index, _ ->
                ImageCard(actualImageUris[index], loadingImages.contains(index))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (!paginationState.isLoading && !loadingNextPage) {
                        CircularProgressIndicator()
                        LaunchedEffect(lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
                            val lastVisibleItemIndex =
                                lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                            val totalItems = placeholders.size - 1
                            if (lastVisibleItemIndex != null && lastVisibleItemIndex >= totalItems) {
                                paginationState = paginationState.copy(
                                    currentPage = paginationState.currentPage + 1,
                                    isLoading = true
                                )
                            }
                        }
                    }
                }
            }

            item{
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
fun ImageCard(uri: Uri?, isLoading: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
    ) {
        SubcomposeAsyncImage(
            model = uri,
            contentDescription = "Compressed Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
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
        val imageIds = MutableList<Long?>(indexesToLoad.size){null}
        val totalCount = cursor.count
        if (totalCount == 0) {
            return emptyList()
        }
        indexesToLoad.forEachIndexed { index, element ->
            if (element < totalCount){
                if (cursor.moveToPosition(element)) {
                    val id = cursor.getLong(idColumn)
                    imageIds[index] = id
                }
            }
        }
        imageIds.forEach { imageId->
            if (imageId != null){
                val contentUri = ContentUris.withAppendedId(queryUri, imageId)
                compressedImageUris.add(contentUri)
            } else{
                compressedImageUris.add(null)
            }
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
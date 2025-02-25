package com.image.resizer.compose

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val TAG = "HomeScreen"
    var selectedImageUris by remember { mutableStateOf(emptyList<Uri>()) }
    val context = LocalContext.current
    var imagePairs by remember { mutableStateOf(listOf<ImagePair>()) }
    val viewModel = ScaleImageViewModel()
    // State to control the popup's visibility
    var showScalePopup by remember { mutableStateOf(false) }
    // State to pass to the popup
    Scaffold(
        topBar = {
            MyTopAppBar(
                selectedImageUris,
                context,
                viewModel,
                onShowScalePopup = {
                    showScalePopup = !showScalePopup
                },
                onUndo = {
                    imagePairs = emptyList()
                }
            ) { compressedUris ->
                imagePairs = selectedImageUris.zip(compressedUris) { original, compressed ->
                    ImagePair(original, compressed)
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Column(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Companion.CenterHorizontally
            ) {

                if (imagePairs.isNotEmpty()) {
                    ImageComparisonGrid(imagePairs)
                } else if (selectedImageUris.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(selectedImageUris) { uri ->

                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.Companion
                                    .padding(4.dp)
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        2.dp,
                                        Color.Companion.Gray,
                                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    )

                            )
                        }
                    }
                }
                GalleryApp(viewModel = viewModel, onImagesSelected = { uris ->
                    selectedImageUris = uris
                })

            }

        }
    }

    // Conditionally display the popup
    if (showScalePopup) {
        val originalDimensions = listOf(Pair(1000, 2000))
        // Implement image scaling logic here
        AnimatedVisibility(
            visible = showScalePopup,
            enter = fadeIn(animationSpec = tween(durationMillis = 2000)) + expandVertically(
                expandFrom = Alignment.Companion.CenterVertically,
                animationSpec = tween(durationMillis = 1300)
            ),
            exit = fadeOut(animationSpec = tween(durationMillis = 2000)) + shrinkVertically(
                shrinkTowards = Alignment.Companion.CenterVertically,
                animationSpec = tween(durationMillis = 1300)
            ),
        ) {
            ScaleImagePopup(showScalePopup, onDismiss = {
                showScalePopup = false
            }, originalDimensions, viewModel = viewModel, onScale = {
                it.forEachIndexed { index, it ->

                    Log.d(TAG," $it here  ${originalDimensions[index]} ")
                }


            })
        }
    }

}
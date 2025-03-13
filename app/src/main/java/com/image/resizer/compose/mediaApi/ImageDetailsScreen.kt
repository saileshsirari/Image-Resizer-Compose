package com.image.resizer.compose.mediaApi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.image.resizer.compose.HomeScreenViewModel
import com.image.resizer.compose.ImageComparisonView
import com.image.resizer.compose.ImageItem

@Composable
fun ImageDetailsScreen(selectedImageItem: ImageItem) {
    AnimatedVisibility(
        visible = true,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 1500)
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 1500)
        ) + fadeOut()
    ) {
        ImageComparisonView(imageItem = selectedImageItem)
    }

}
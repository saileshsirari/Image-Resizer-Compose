@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package com.image.resizer.compose.mediaApi.util


import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.contentSize
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.image.resizer.compose.mediaApi.model.Album
import com.image.resizer.compose.mediaApi.model.Media

context(SharedTransitionScope)
@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun <T: Media> Modifier.mediaSharedElement(
    media: T,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = mediaSharedElement(key = media.idLessKey, animatedVisibilityScope = animatedVisibilityScope)

context(SharedTransitionScope)
@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.mediaSharedElement(
    album: Album,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = mediaSharedElement(key = album.idLessKey, animatedVisibilityScope = animatedVisibilityScope)

context(SharedTransitionScope)
@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun Modifier.mediaSharedElement(
    key: String,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier {
    val shouldAnimate by remember{ mutableStateOf(true)}
    val boundsModifier = sharedBounds(
        rememberSharedContentState(key = "media_$key"),
        animatedVisibilityScope = animatedVisibilityScope,
        placeHolderSize = contentSize,
        boundsTransform = { _, _ -> tween(250) }
    )
    return if (shouldAnimate) boundsModifier else Modifier
}
/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.util.isVideo

@Stable
@NonRestartableComposable
@Composable
fun <T: Media> MediaPreviewComponent(
    media: T?,
    modifier: Modifier = Modifier,
    uiEnabled: Boolean,
    onItemClick: () -> Unit,
    onSwipeDown: () -> Unit,
    offset: IntOffset,
) {
    if (media != null) {
        Box(
            modifier = Modifier.fillMaxSize().offset { offset },
        ) {
            AnimatedVisibility(
                modifier = Modifier.fillMaxSize(),
                visible = media.isVideo,
                enter = fadeIn(),
                exit = fadeOut()
            ) {

            }

            AnimatedVisibility(
                visible = !media.isVideo,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ZoomablePagerImage(
                    modifier = modifier,
                    media = media,
                    uiEnabled = uiEnabled,
                    onItemClick = onItemClick,
                    onSwipeDown = onSwipeDown
                )
            }
        }
    }
}
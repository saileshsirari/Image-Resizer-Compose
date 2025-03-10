/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.AsyncImage
import com.image.resizer.compose.R
import com.image.resizer.compose.mediaApi.model.Album
import com.image.resizer.compose.mediaApi.util.formatSize
import com.image.resizer.compose.mediaApi.util.rememberFeedbackManager
import kotlinx.coroutines.launch

@Composable
fun AlbumComponent(
    modifier: Modifier = Modifier,
    thumbnailModifier: Modifier = Modifier,
    album: Album,
    isEnabled: Boolean = true,
    onItemClick: (Album) -> Unit,
    onMoveAlbumToTrash: ((Album) -> Unit)? = null,
    onTogglePinClick: ((Album) -> Unit)? = null,
    onToggleIgnoreClick: ((Album) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .alpha(if (isEnabled) 1f else 0.4f)
            .padding(horizontal = 8.dp),
    ) {
        if (onTogglePinClick != null) {
            val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
            val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer
            val primaryContainer = MaterialTheme.colorScheme.primaryContainer
            val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

            LaunchedEffect(onToggleIgnoreClick) {
                if (onToggleIgnoreClick != null) {

                }
            }

        }
        Box(
            modifier = Modifier
                .aspectRatio(1f)
        ) {
            AlbumImage(
                modifier = thumbnailModifier,
                album = album,
                isEnabled = isEnabled,
                onItemClick = onItemClick,
                onItemLongClick = if (onTogglePinClick != null) {
                    {

                    }
                } else null
            )
            if (album.isOnSdcard) {
                Icon(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(24.dp)
                        .align(Alignment.BottomEnd),
                    imageVector = Icons.Outlined.SdCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            modifier = Modifier
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp),
            text = album.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
        if (album.count > 0) {
            Text(
                modifier = Modifier
                    .padding(top = 2.dp, bottom = 16.dp)
                    .padding(horizontal = 16.dp),
                text = pluralStringResource(
                    id = R.plurals.item_count,
                    count = album.count.toInt(),
                    album.count
                ) + " (${formatSize(album.size)})",
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium,
            )
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumImage(
    modifier: Modifier = Modifier,
    album: Album,
    isEnabled: Boolean,
    onItemClick: (Album) -> Unit,
    onItemLongClick: ((Album) -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val radius = if (isPressed.value) 32.dp else 16.dp
    val cornerRadius by animateDpAsState(targetValue = radius, label = "cornerRadius")
    val feedbackManager = rememberFeedbackManager()
    if (album.id == -200L && album.count == 0L) {
        Icon(
            imageVector = Icons.Outlined.AddCircleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .alpha(0.8f)
                .clip(RoundedCornerShape(cornerRadius))
                .combinedClickable(
                    enabled = isEnabled,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = { onItemClick(album) },
                    onLongClick = {
                        onItemLongClick?.let {
                            feedbackManager.vibrate()
                            it(album)
                        }
                    }
                )
                .padding(48.dp)
        )
    } else {
        AsyncImage(
            modifier = modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .clip(RoundedCornerShape(cornerRadius))
                .combinedClickable(
                    enabled = isEnabled,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = { onItemClick(album) },
                    onLongClick = {
                        onItemLongClick?.let {
                            feedbackManager.vibrate()
                            it(album)
                        }
                    }
                ),
            uri = album.uri.toString(),
            contentDescription = album.label,
            contentScale = ContentScale.Crop,
        )
    }
}
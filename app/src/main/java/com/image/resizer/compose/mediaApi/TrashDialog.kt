/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.resize.Scale
import apps.sai.com.imageresizer.R
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.util.Constants.Animation.enterAnimation
import com.image.resizer.compose.mediaApi.util.Constants.Animation.exitAnimation
import com.image.resizer.compose.mediaApi.util.getUri
import com.image.resizer.compose.mediaApi.util.rememberFeedbackManager
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun <T : Media> TrashDialog(
    appBottomSheetState: AppBottomSheetState,
    data: List<T>,
    action: TrashDialogAction,
    onConfirm: suspend (List<T>) -> Unit
) {
    val dataCopy = data.toMutableStateList()
    var confirmed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler(
        appBottomSheetState.isVisible && !confirmed
    ) {
        scope.launch {
            confirmed = false
            appBottomSheetState.hide()
        }
    }
    if (appBottomSheetState.isVisible) {
        confirmed = false
        ModalBottomSheet(
            sheetState = appBottomSheetState.sheetState,
            onDismissRequest = {
                scope.launch {
                    appBottomSheetState.hide()
                }
            },
            dragHandle = { DragHandle() },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
            val tertiaryOnContainer = MaterialTheme.colorScheme.onTertiaryContainer
            val mainButtonDefaultText = stringResource(R.string.action_confirm)
            val mainButtonConfirmText = stringResource(R.string.action_confirmed)
            val mainButtonText = remember(confirmed) {
                if (confirmed) mainButtonConfirmText else mainButtonDefaultText
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedVisibility(
                    visible = !confirmed,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    val text = when (action) {
                        TrashDialogAction.TRASH -> stringResource(R.string.dialog_to_trash)
                        TrashDialogAction.DELETE -> stringResource(R.string.dialog_delete)
                    }
                    Column {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontStyle = MaterialTheme.typography.titleLarge.fontStyle,
                                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                        letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing
                                    )
                                ) {
                                    append(text)
                                }
                                append("\n")
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                        letterSpacing = MaterialTheme.typography.bodyMedium.letterSpacing
                                    )
                                ) {
                                    append(stringResource(R.string.s_items, dataCopy.size))
                                }
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                        )
                    }
                }

                AnimatedVisibility(
                    visible = confirmed,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    val text =
                        when (action) {
                            TrashDialogAction.TRASH -> stringResource(
                                R.string.trashing_items,
                                dataCopy.size
                            )

                            TrashDialogAction.DELETE -> stringResource(
                                R.string.deleting_items,
                                dataCopy.size
                            )
                        }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    )
                }

                val mediaPair = dataCopy.mediaPair()

                AnimatedVisibility(visible = mediaPair.second.isNotEmpty() && !confirmed) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = Shapes.large
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = stringResource(R.string.trash_incompatible_title),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.trash_incompatible_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val alpha by animateFloatAsState(
                    targetValue = if (!confirmed) 1f else 0.5f,
                    label = "alphaAnimation"
                )

                val alignment = if (dataCopy.size == 1) {
                    Alignment.CenterHorizontally
                } else Alignment.Start

                LazyRow(
                    modifier = Modifier
                        .alpha(alpha)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, alignment),
                ) {
                    if (dataCopy.size > 1) {
                        item {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    }
                    items(
                        items = dataCopy,
                        key = { UUID.randomUUID().toString() },
                        contentType = { it.mimeType }
                    ) {
                        val context = LocalContext.current
                        val longPressText = stringResource(R.string.long_press_to_remove)
                        val canBeTrashed = it.canBeTrashed()
                        val borderWidth = if (canBeTrashed) 0.5.dp else 2.dp
                        val borderColor =
                            if (canBeTrashed) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.error
                        val shape = if (canBeTrashed) Shapes.large else Shapes.extraLarge
                        val feedbackManager = rememberFeedbackManager()
                        Box(
                            modifier = Modifier
                                .animateItem()
                                .size(width = 80.dp, height = 120.dp)
                                .clip(shape)
                                .border(
                                    width = borderWidth,
                                    color = borderColor,
                                    shape = shape
                                )
                                .combinedClickable(
                                    enabled = !confirmed,
                                    onLongClick = {
                                        feedbackManager.vibrate()
                                        scope.launch {
                                            dataCopy.remove(it)
                                            if (dataCopy.isEmpty()) {
                                                appBottomSheetState.hide()
                                                dataCopy.addAll(data)
                                            }
                                        }
                                    },
                                    onClick = {
                                        feedbackManager.vibrateStrong()
                                        Toast
                                            .makeText(context, longPressText, Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                )
                        ) {
                            AsyncImage(
                                modifier = Modifier.fillMaxSize(),
                                request = ComposableImageRequest(it.getUri().toString()) {
                                    scale(Scale.CENTER_CROP)
                                    setExtra(
                                        key = "mediaKey",
                                        value = it.toString(),
                                    )
                                    setExtra(
                                        key = "realMimeType",
                                        value = it.mimeType,
                                    )
                                },
                                contentDescription = it.label,
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement
                        .spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                    AnimatedVisibility(visible = !confirmed) {
                        Button(
                            onClick = {
                                scope.launch {
                                    appBottomSheetState.hide()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tertiaryContainer,
                                contentColor = tertiaryOnContainer
                            )
                        ) {
                            Text(text = stringResource(R.string.action_cancel))
                        }
                    }
                    Button(
                        enabled = !confirmed,
                        onClick = {
                            confirmed = true
                            scope.launch {
                                onConfirm.invoke(dataCopy)
                                appBottomSheetState.hide()
                            }
                        }
                    ) {
                        Text(text = mainButtonText)
                    }
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

enum class TrashDialogAction {
    TRASH, DELETE
}
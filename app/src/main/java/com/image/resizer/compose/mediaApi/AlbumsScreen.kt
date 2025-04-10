/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.image.resizer.compose.R
import com.image.resizer.compose.mediaApi.model.Album
import com.image.resizer.compose.mediaApi.model.AlbumState
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.model.MediaState
import com.image.resizer.compose.mediaApi.util.Constants.Animation.enterAnimation
import com.image.resizer.compose.mediaApi.util.Constants.Animation.exitAnimation
import com.image.resizer.compose.mediaApi.util.mediaSharedElement
import com.image.resizer.compose.mediaApi.util.rememberActivityResult

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumsScreen(
    mediaState: State<MediaState<Media.UriMedia>>,
    albumsState: State<AlbumState>,
    paddingValues: PaddingValues,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {

    var finalPaddingValues by remember(paddingValues) { mutableStateOf(paddingValues) }

    Scaffold(
        modifier = Modifier.padding(
            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
        ),

    ) { innerPaddingValues ->
        LaunchedEffect(innerPaddingValues) {
            finalPaddingValues = PaddingValues(
                top = innerPaddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 16.dp + 64.dp
            )
        }
            LazyVerticalGrid(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxSize(),
                columns = GridCells.Fixed(2),
                contentPadding = finalPaddingValues,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {


                items(
                    items = albumsState.value.albumsUnpinned,
                    key = { item -> item.toString() }
                ) { item ->
                    val trashResult = rememberActivityResult()
                    with(sharedTransitionScope) {
                        AlbumComponent(
                            modifier = Modifier
                                .animateItem(),
                            thumbnailModifier = Modifier
                                .mediaSharedElement(
                                    album = item,
                                    animatedVisibilityScope = animatedContentScope
                                ),
                            album = item,
                            onItemClick = onAlbumClick,
                            onTogglePinClick = onAlbumLongClick,
                            onMoveAlbumToTrash = {
//                                onMoveAlbumToTrash(trashResult, it)
                            }
                        )
                    }
                }

                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "albumDetails"
                ) {
                    AnimatedVisibility(
                        visible = mediaState.value.media.isNotEmpty() && albumsState.value.albums.isNotEmpty(),
                        enter = enterAnimation,
                        exit = exitAnimation
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .padding(vertical = 24.dp),
                            text = stringResource(
                                R.string.images_videos,
                                mediaState.value.media.size
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "emptyAlbums"
                ) {
                    AnimatedVisibility(
                        visible = albumsState.value.albums.isEmpty() && albumsState.value.error.isEmpty(),
                        enter = enterAnimation,
                        exit = exitAnimation
                    ) {
                        EmptyAlbum()
                    }
                }

                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "loadingAlbums"
                ) {
                    AnimatedVisibility(
                        visible = albumsState.value.isLoading,
                        enter = enterAnimation,
                        exit = exitAnimation
                    ) {
                        LoadingAlbum()
                    }
                }
            }
        }
    }
    /** Error State Handling Block **/
 /*   AnimatedVisibility(
        visible = albumsState.value.error.isNotEmpty(),
        enter = enterAnimation,
        exit = exitAnimation
    ) {
        Error(errorMessage = albumsState.value.error)
    }*/

    /** ************ **/

/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi

import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.core.BottomSheet
import com.composables.core.SheetDetent.Companion.FullyExpanded
import com.composables.core.rememberBottomSheetState
import com.github.panpf.sketch.BitmapImage
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.sketch
import com.image.resizer.compose.mediaApi.ViewScreenConstants.FullyExpanded
import com.image.resizer.compose.mediaApi.ViewScreenConstants.ImageOnly
import com.image.resizer.compose.mediaApi.model.AlbumState
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.model.MediaState
import com.image.resizer.compose.mediaApi.model.Vault
import com.image.resizer.compose.mediaApi.model.VaultState
import com.image.resizer.compose.mediaApi.util.Constants.Animation.enterAnimation
import com.image.resizer.compose.mediaApi.util.Constants.Animation.exitAnimation
import com.image.resizer.compose.mediaApi.util.Constants.DEFAULT_LOW_VELOCITY_SWIPE_DURATION
import com.image.resizer.compose.mediaApi.util.Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION
import com.image.resizer.compose.mediaApi.util.Constants.Target.TARGET_TRASH
import com.image.resizer.compose.mediaApi.util.FullBrightnessWindow
import com.image.resizer.compose.mediaApi.util.ProvideInsets
import com.image.resizer.compose.mediaApi.util.getDate
import com.image.resizer.compose.mediaApi.util.getUri
import com.image.resizer.compose.mediaApi.util.isImage
import com.image.resizer.compose.mediaApi.util.isVideo
import com.image.resizer.compose.mediaApi.util.mediaSharedElement
import com.image.resizer.compose.mediaApi.util.printWarning
import com.image.resizer.compose.mediaApi.util.readUriOnly
import com.image.resizer.compose.mediaApi.util.rememberGestureNavigationEnabled
import com.image.resizer.compose.mediaApi.util.rememberNavigationBarHeight
import com.image.resizer.compose.mediaApi.util.rememberWindowInsetsController
import com.image.resizer.compose.mediaApi.util.setHdrMode
import com.image.resizer.compose.theme.BlackScrim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

@Composable
fun <T> rememberedDerivedState(
    key: Any? = Unit,
    block: @DisallowComposableCalls () -> T
): State<T> {
    return remember(key) {
        derivedStateOf(block)
    }
}

@Composable
fun <T> rememberedDerivedState(
    vararg keys: Any? = arrayOf(Unit),
    block: @DisallowComposableCalls () -> T
): State<T> {
    return remember(keys) {
        derivedStateOf(block)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <T : Media> MediaViewScreen(
    navigateUp: () -> Unit,
    toggleRotate: () -> Unit,
    paddingValues: PaddingValues,
    isStandalone: Boolean = false,
    mediaId: Long,
    target: String? = null,
    mediaState: State<MediaState<out T>>,
    albumsState: State<AlbumState> = remember { mutableStateOf(AlbumState()) },
    handler: MediaHandleUseCase?,
    vaultState: State<VaultState>,
    addMedia: (Vault, T) -> Unit,
    restoreMedia: ((Vault, T, () -> Unit) -> Unit)? = null,
    deleteMedia: ((Vault, T, () -> Unit) -> Unit)? = null,
    currentVault: Vault? = null,
    navigate: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) = ProvideInsets {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val windowInsetsController = rememberWindowInsetsController()

    val initialPage by rememberedDerivedState(mediaId, mediaState.value) {
        mediaState.value.media.indexOfFirst { it.id == mediaId }.coerceAtLeast(0)
    }
    var currentPage by rememberSaveable(initialPage) { mutableIntStateOf(initialPage) }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0f,
        pageCount = mediaState.value.media::size
    )

    val currentMedia by rememberedDerivedState(mediaState.value) {
        mediaState.value.media.getOrNull(currentPage)
    }

    var shouldForcePage by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialPage, currentPage, mediaState.value) {
        if (currentPage == 0) {
            if (mediaState.value.isLoading) {
                shouldForcePage = true
            } else {
                if (initialPage != 0 && currentPage != initialPage && shouldForcePage) {
                    pagerState.scrollToPage(initialPage)
                }
                shouldForcePage = false
            }
        }
    }

    val currentDateFormat by remember { mutableStateOf("") }

    val currentDate by rememberedDerivedState {
        currentMedia?.definedTimestamp?.getDate(currentDateFormat) ?: ""
    }
    val isReadOnly by rememberedDerivedState { currentMedia?.readUriOnly == true }
    val showInfo by rememberedDerivedState { currentMedia?.trashed == 0 && !isReadOnly }

    var showUI by rememberSaveable { mutableStateOf(true) }

    BackHandler(!showUI) {
        windowInsetsController.toggleSystemBars(show = true)
        navigateUp()
    }
    var sheetHeightDp by remember { mutableStateOf(1.dp) }
    var lastSheetHeightDp by rememberSaveable { mutableFloatStateOf(0f) }

    val configuration = LocalConfiguration.current
    var lastOrientation by rememberSaveable(configuration.orientation) {
        mutableIntStateOf(
            configuration.orientation
        )
    }
    val isLandscape = remember(configuration) {
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
    val isGestureEnabled = rememberGestureNavigationEnabled()
    // Extra padding for navigation bar with 3/2-buttons
    val extraPaddingWithNavButtons by remember(isLandscape, isGestureEnabled) {
        mutableStateOf(
            if (!isGestureEnabled && !isLandscape) {
                32.dp
            } else 0.dp
        )
    }
    val navigationBarHeight = rememberNavigationBarHeight()
    val bottomBarHeightDefault by remember(isGestureEnabled, isLandscape) {
        mutableStateOf(
            if (!isGestureEnabled && isLandscape) 84.dp
            else ViewScreenConstants.BOTTOM_BAR_HEIGHT
        )
    }

    val imageOnlyDetent = remember(bottomBarHeightDefault, extraPaddingWithNavButtons) {
        ImageOnly { bottomBarHeightDefault + extraPaddingWithNavButtons }
    }

    val sheetState = rememberBottomSheetState(
        initialDetent = imageOnlyDetent,
        detents = listOf(imageOnlyDetent, FullyExpanded { sheetHeightDp = it })
    )

    val userScrollEnabled by rememberedDerivedState { sheetState.currentDetent != FullyExpanded }

    var storedNormalizationTarget by rememberSaveable(configuration.orientation) {
        mutableFloatStateOf(0f)
    }

    val isNormalizationTargetSet by rememberedDerivedState(
        bottomBarHeightDefault,
        currentPage,
        pagerState.currentPage,
        configuration.orientation,
        mediaState.value,
        sheetHeightDp.value,
        lastSheetHeightDp,
        lastOrientation,
        shouldForcePage
    ) {
        lastOrientation == configuration.orientation
                && lastSheetHeightDp.dp == sheetHeightDp
                && currentPage == pagerState.currentPage
                && storedNormalizationTarget > 0f
                && storedNormalizationTarget < 0.7f
                && !mediaState.value.isLoading
                && !shouldForcePage
    }

    LaunchedEffect(sheetHeightDp.value, configuration.orientation) {
        if (!isNormalizationTargetSet) {
            lastSheetHeightDp = sheetHeightDp.value
            lastOrientation = configuration.orientation
        }
    }

    val normalizationTarget by rememberedDerivedState(
        isNormalizationTargetSet,
        mediaState.value,
        currentPage,
        sheetState.offset,
        storedNormalizationTarget
    ) {
        if (isNormalizationTargetSet) {
            storedNormalizationTarget
        } else {
            val newOffset = sheetState.offset
            if (newOffset > 0f && newOffset < 1f) {
                storedNormalizationTarget = newOffset
            }
            storedNormalizationTarget
        }
    }

    LaunchedEffect(shouldForcePage) {
        if (!shouldForcePage) {
            storedNormalizationTarget = sheetState.offset
        }
    }

    val normalizedOffset by rememberedDerivedState(
        normalizationTarget,
        isNormalizationTargetSet
    ) {
        if (isNormalizationTargetSet) {
            sheetState.offset.normalize(minValue = normalizationTarget)
        } else 0f
    }

    LaunchedEffect(mediaState.value) {
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            if (!mediaState.value.isLoading && mediaState.value.media.isEmpty() && !isStandalone) {
                windowInsetsController.toggleSystemBars(show = true)
                navigateUp()
            }
            if (!mediaState.value.isLoading) {
                currentPage = page
            }
        }
    }

    // set HDR Gain map
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        LaunchedEffect(mediaState.value) {
            withContext(Dispatchers.IO) {
                snapshotFlow { pagerState.currentPage }.collectLatest {
                    printWarning("Trying to set HDR mode for page $it")
                    if (currentMedia?.isImage == true) {
                        val request = ImageRequest(context, currentMedia?.getUri().toString()) {
                            setExtra(
                                key = "mediaKey",
                                value = currentMedia.toString(),
                            )
                            setExtra(
                                key = "realMimeType",
                                value = currentMedia?.mimeType,
                            )
                        }
                        val result = context.sketch.execute(request)
                        (result.image as? BitmapImage)?.bitmap?.let { bitmap ->
                            val hasGainmap = bitmap.hasGainmap()
                            withContext(Dispatchers.Main.immediate) {
                                context.setHdrMode(hasGainmap)
                            }
                            printWarning("Setting HDR Mode to $hasGainmap")
                        } ?: printWarning("Resulting image null")
                    } else {
                        withContext(Dispatchers.Main.immediate) {
                            context.setHdrMode(false)
                        }
                        printWarning("Not an image, skipping")
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                printWarning("Disposing HDR Mode")
                context.setHdrMode(false)
            }
        }
    }

    val bottomPadding =
        remember(configuration.orientation) {
            //if (!isGestureEnabled && isLandscape) 0.dp
            //else
            paddingValues.calculateBottomPadding()
        }

    FullBrightnessWindow {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY =
                            -((sheetHeightDp -
                                    bottomBarHeightDefault -
                                    bottomPadding -
                                    extraPaddingWithNavButtons -
                                    if (!isGestureEnabled && isLandscape) navigationBarHeight else 0.dp
                                    ).toPx() * normalizedOffset)
                    },
                userScrollEnabled = userScrollEnabled,
                state = pagerState,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = tween(
                        easing = FastOutLinearInEasing,
                        durationMillis = DEFAULT_LOW_VELOCITY_SWIPE_DURATION
                    ),
                    snapPositionalThreshold = 0.3f
                ),
                key = { index ->
                    mediaState.value.media.getOrNull(index) ?: "empty"
                },
                pageSpacing = 16.dp,
                beyondViewportPageCount = 0
            ) { index ->
                val media by rememberedDerivedState(mediaState.value) {
                    mediaState.value.media.getOrNull(
                        index
                    )
                }
                AnimatedVisibility(
                    visible = media != null,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    var offset by remember {
                        mutableStateOf(IntOffset(0, 0))
                    }
                    with(sharedTransitionScope) {
                        MediaPreviewComponent(
                            modifier = Modifier
                                .mediaSharedElement(
                                    media = media!!,
                                    animatedVisibilityScope = animatedContentScope
                                ),
                            media = media,
                            uiEnabled = showUI,
                            onSwipeDown = {
                                windowInsetsController.toggleSystemBars(show = true)
                                navigateUp()
                            },
                            offset = offset,
                            onItemClick = {
                                if (sheetState.currentDetent == imageOnlyDetent) {
                                    showUI = !showUI
                                    windowInsetsController.toggleSystemBars(showUI)
                                }
                            }
                        )
                    }
                }
            }
            MediaViewAppBar(
                modifier = Modifier.padding(
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                ),
                showUI = showUI,
                showInfo = showInfo,
                showDate = remember(currentMedia) {
                    currentMedia?.timestamp != 0L
                },
                currentDate = currentDate,
                paddingValues = paddingValues,
                onShowInfo = {
                    scope.launch {
                        if (showUI) {
                            if (sheetState.currentDetent == imageOnlyDetent) {
                                sheetState.animateTo(FullyExpanded)
                            } else {
                                sheetState.animateTo(imageOnlyDetent)
                            }
                        }
                    }
                },
                onGoBack = {
                    scope.launch {
                        if (sheetState.currentDetent == FullyExpanded) {
                            sheetState.animateTo(imageOnlyDetent)
                        } else {
                            navigateUp()
                        }
                    }
                }
            )
            LaunchedEffect(showUI) {
                if (!showUI && (sheetState.currentDetent == FullyExpanded || sheetState.targetDetent == FullyExpanded)) {
                    sheetState.animateTo(imageOnlyDetent)
                }
            }
            BackHandler(sheetState.currentDetent == FullyExpanded) {
                scope.launch {
                    sheetState.animateTo(imageOnlyDetent)
                }
            }
            val bottomSheetAlpha by animateFloatAsState(
                targetValue = if (showUI) 1f else 0f,
                animationSpec = tween(DEFAULT_TOP_BAR_ANIMATION_DURATION),
                label = "MediaViewActionsAlpha"
            )
            BottomSheet(
                state = sheetState,
                enabled = showUI && target != TARGET_TRASH && showInfo,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        alpha = bottomSheetAlpha
                    }
                    .padding(
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                    )
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val actionsAlpha by animateFloatAsState(
                        targetValue = 1f - normalizedOffset,
                        label = "MediaViewActions2Alpha"
                    )
                    AnimatedVisibility(
                        visible = currentMedia != null,
                        enter = enterAnimation,
                        exit = exitAnimation
                    ) {
                        Row(
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = actionsAlpha
                                    translationY =
                                        bottomBarHeightDefault.toPx() * normalizedOffset
                                }
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, BlackScrim)
                                    )
                                )
                                .padding(
                                    bottom = bottomPadding + extraPaddingWithNavButtons
                                )
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                           /* MediaViewActions2(
                                currentMedia = currentMedia,
                                handler = handler,
                                showDeleteButton = !isReadOnly,
                                enabled = showUI,
                                deleteMedia = deleteMedia,
                                restoreMedia = restoreMedia,
                                currentVault = currentVault
                            )*/
                        }
                    }

                /*    MediaViewDetails(
                        albumsState = albumsState,
                        vaultState = vaultState,
                        currentMedia = currentMedia,
                        handler = handler,
                        addMediaToVault = addMedia,
                        restoreMedia = restoreMedia,
                        currentVault = currentVault,
                        navigate = navigate,
                    )*/
                }
            }
        }
    }

}
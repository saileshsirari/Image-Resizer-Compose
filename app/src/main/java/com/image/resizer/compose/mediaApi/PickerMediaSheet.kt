package com.image.resizer.compose.mediaApi

import android.app.Activity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.image.resizer.compose.HomeScreenViewModel
import com.image.resizer.compose.R
import com.image.resizer.compose.Screen
import com.image.resizer.compose.mediaApi.model.AlbumState
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.model.MediaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun <T : Media> PickerMediaSheet(
    sheetState: AppBottomSheetState,
    albumsState: State<AlbumState>,
    paddingValues: PaddingValues,
    mediaState: State<MediaState<Media.UriMedia>>,
    mediaList: List<T>,
    homeScreenViewModel: HomeScreenViewModel,
    activity: Activity,
) {
    var progress by remember(mediaList) { mutableFloatStateOf(0f) }
    val navController = rememberNavController()
    var hideSheet by remember { mutableStateOf(false) }
    val selectedMediaRepository = SelectedMediaRepository(context = LocalContext.current)
    val scope = rememberCoroutineScope()
    val mediaRepository = MediaRepositoryImpl(LocalContext.current)
    val mediaHandleUseCase =
        MediaHandleUseCase(repository = mediaRepository, context = LocalContext.current)
    val albumsViewModel = AlbumsViewModel(mediaRepository, mediaHandleUseCase)

    if (sheetState.isVisible) {
        if (hideSheet) {
            LaunchedEffect(Unit) {
                sheetState.hide()
            }
        }
        val prop = remember(progress) {
            val shouldDismiss = progress == 0f
            ModalBottomSheetProperties(
                securePolicy = SecureFlagPolicy.Inherit,
                shouldDismissOnBackPress = shouldDismiss
            )
        }
        ModalBottomSheet(
            sheetState = sheetState.sheetState,
            onDismissRequest = {
                scope.launch {
                    if (progress == 0f) {
                        sheetState.hide()
                    } else {
                        sheetState.show()
                    }
                }
            },
            properties = prop,
            dragHandle = { DragHandle() },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {

            SharedTransitionLayout {

                NavHost(
                    navController = navController,
                    startDestination = Screen.AlbumsScreen.route
                ) {
                    composable(
                        Screen.AlbumsScreen.route
                    ) {
                        AlbumsScreen(
                            mediaState = mediaState,
                            albumsState = albumsState,
                            paddingValues = paddingValues,
                            onAlbumClick =
                                albumsViewModel.onAlbumClick {
                                    navController.navigate(it) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            onAlbumLongClick = {

                            },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedContentScope = this
                        )
                    }
                    composable(
                        route = Screen.AlbumViewScreen.albumAndName(),
                        arguments = listOf(
                            navArgument(name = "albumId") {
                                type = NavType.LongType
                                defaultValue = -1
                            },
                            navArgument(name = "albumName") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) { backStackEntry ->
                        val appName = stringResource(id = R.string.app_name)
                        val argumentAlbumName = remember(backStackEntry) {
                            backStackEntry.arguments?.getString("albumName") ?: appName
                        }
                        val argumentAlbumId = remember(backStackEntry) {
                            backStackEntry.arguments?.getLong("albumId") ?: -1
                        }


                        val context = LocalContext.current

                        val hideTimeline by remember { mutableStateOf(true) }
                        val mediaState =
                            albumsViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

                        TimelineScreen(
                            paddingValues = paddingValues,
                            albumId = argumentAlbumId,
                            albumName = argumentAlbumName,
                            handler = albumsViewModel.handler,
                            mediaState = mediaState,
                            albumsState = albumsState,
                            selectionState = albumsViewModel.multiSelectState,
                            selectedMedia = albumsViewModel.selectedPhotoState,
                            allowNavBar = false,
                            allowHeaders = !hideTimeline,
                            enableStickyHeaders = !hideTimeline,
                            toggleSelection = albumsViewModel::toggleSelection,
                            activity = activity,
                            navigate = {
                                navController.navigate(it) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            navigateUp = {
                                navController.navigateUp()
                            },
                            toggleNavbar = {

                            },
                            isScrolling = mutableStateOf(false),
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedContentScope = this,
                            selectedMediaRepository = selectedMediaRepository,
                            onCompressClick = {

                                homeScreenViewModel.handlePickedImages(it, context) {
                                    hideSheet = true
                                }

                            }
                        )
                    }
                }

            }
        }
    }
}
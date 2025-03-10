package com.image.resizer.compose.mediaApi

import android.R.attr.type
import android.app.Activity
import android.net.http.SslCertificate.restoreState
import android.os.Environment
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.image.resizer.compose.mediaApi.model.Album
import com.image.resizer.compose.mediaApi.model.AlbumState
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.model.MediaState
import com.image.resizer.compose.mediaApi.util.Constants
import com.image.resizer.compose.mediaApi.util.Constants.albumCellsList
import com.image.resizer.compose.mediaApi.util.toastError
import com.image.resizer.compose.mediaApi.util.volume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun <T : Media> CopyMediaSheet(
    sheetState: AppBottomSheetState,
    albumsState: State<AlbumState>,
    handler: MediaHandleUseCase,
    paddingValues: PaddingValues,
    mediaState: State<MediaState<Media.UriMedia>>,
    mediaList: List<T>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    homeScreenViewModel: HomeScreenViewModel,
    activity: Activity,
    onFinish: () -> Unit,
) {
    val toastError = toastError()
    var progress by remember(mediaList) { mutableFloatStateOf(0f) }
    var newPath by remember(mediaList) { mutableStateOf("") }
    val navController = rememberNavController()
    val newAlbumSheetState = rememberAppBottomSheetState()
    val mutex = Mutex()
    var hideSheet by remember { mutableStateOf(false) }
    val selectedMediaRepository = SelectedMediaRepository(context = LocalContext.current)
    val scope = rememberCoroutineScope()

    var albumId by remember { mutableStateOf(-1L) }
    var albumName by remember { mutableStateOf("") }
    val mediaRepository = MediaRepositoryImpl(LocalContext.current)
    val mediaHandleUseCase =
        MediaHandleUseCase(repository = mediaRepository, context = LocalContext.current)
    val albumsViewModel = AlbumsViewModel(mediaRepository, mediaHandleUseCase)
    val timelineViewModel = MediaViewModel(mediaRepository, mediaHandleUseCase)
    fun copyMedia(path: String) {
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                newPath = path
                async {
                    mediaList.forEachIndexed { i, media ->
                        /* if (handler.copyMedia(media, newPath)) {
                             progress = (i + 1f) / mediaList.size
                         }*/
                    }
                }.await()
                newPath = ""
                if (progress == 1f) {
                    sheetState.hide()
                    progress = 0f
                    onFinish()
                } else {
                    toastError.show()
                    delay(1000)
                    sheetState.hide()
                    progress = 0f
                }
            }
        }
    }

    if (sheetState.isVisible) {
        if(hideSheet){
            scope.launch {
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
                        val vm = AlbumsViewModel(
                            repository = mediaRepository,
                            mediaHandleUseCase
                        ).apply {
                            albumId = argumentAlbumId
                        }

                        val context = LocalContext.current

                        val hideTimeline by remember { mutableStateOf(true) }
                        val mediaState =
                            vm.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

                        TimelineScreen(
                            paddingValues = paddingValues,
                            albumId = argumentAlbumId,
                            albumName = argumentAlbumName,
                            handler = vm.handler,
                            mediaState = mediaState,
                            albumsState = albumsState,
                            selectionState = vm.multiSelectState,
                            selectedMedia = vm.selectedPhotoState,
                            allowNavBar = false,
                            allowHeaders = !hideTimeline,
                            enableStickyHeaders = !hideTimeline,
                            toggleSelection = vm::toggleSelection,
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
                                   hideSheet =true
                                }

                            }
                        )
                    }
                }

            }
        }
    }


}
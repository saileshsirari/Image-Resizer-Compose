package com.image.resizer.compose

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.image.resizer.compose.ImageReplacer.getBitmapFromUri
import com.image.resizer.compose.Screen.ImageDetailScreen
import com.image.resizer.compose.mediaApi.AlbumsViewModel
import com.image.resizer.compose.mediaApi.ImageDetailsScreen
import com.image.resizer.compose.mediaApi.MediaHandleUseCase
import com.image.resizer.compose.mediaApi.MediaRepositoryImpl
import com.image.resizer.compose.mediaApi.MediaViewModel
import com.image.resizer.compose.mediaApi.TimelineScreen
import com.image.resizer.compose.mediaApi.model.Media.UriMedia
import com.image.resizer.compose.mediaApi.model.MediaState
import com.image.resizer.compose.mediaApi.util.Constants.Animation.navigateInAnimation
import com.image.resizer.compose.mediaApi.util.Constants.Animation.navigateUpAnimation
import com.image.resizer.compose.mediaApi.util.Constants.CUSTOM_FOLDER_NAME
import com.image.resizer.compose.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

// Data class to hold original and compressed image URIs
data class ImagePair(val originalImageItem: ImageItem, val transFormedImageItem: ImageItem)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(tonalElevation = 5.dp) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Navigation(navController, innerPadding)
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val items = listOf(
        Screen.Home,
        Screen.MyImages
    )
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon ?: Icons.Filled.MoreVert,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title ?: "") },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Navigation(navController: NavHostController, innerPadding: PaddingValues) {
    val mediaRepository = MediaRepositoryImpl(LocalContext.current)
    val mediaHandleUseCase =
        MediaHandleUseCase(repository = mediaRepository)
    val albumsViewModel = AlbumsViewModel(mediaRepository, mediaHandleUseCase).apply {
        albumId = -1
    }
    val albumsState =
        albumsViewModel.albumsFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
    val homeScreenViewModel = HomeScreenViewModel(mediaHandleUseCase)
    val activity = LocalActivity.current as Activity

    val context = LocalContext.current

    val hideTimeline by remember { mutableStateOf(true) }

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation },
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(Screen.Home.route) {

                val mediaState =
                    albumsViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
                HomeScreen(
                    homeScreenViewModel = homeScreenViewModel,
                    albumsViewModel = albumsViewModel,
                    mediaState = mediaState,
                    selectionState = albumsViewModel.multiSelectState,
                    selectedMedia = albumsViewModel.selectedPhotoState,
                    paddingValues = innerPadding,
                    navController = navController,
                    albumsState = albumsState,
                    onItemClick = {
                        navController.navigate(Screen.AlbumsScreen.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    navigate = {
                        navController.navigate(it) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    handler = albumsViewModel.handler,
                    navigateUp = {
                        navController.navigateUp()
                    },
                )
            }
            composable(
                route = Screen.MyImages.route
            ) { backStackEntry ->

                val myImages =
                    albumsState.value.albums.firstOrNull { it.label == CUSTOM_FOLDER_NAME }
                if (myImages != null) {
                    var myImagesVm = MediaViewModel(
                        repository = mediaRepository,
                        handler = mediaHandleUseCase
                    ).apply {
                        albumId = myImages.id
                    }
                    val myImagesMediaState =
                        myImagesVm.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)


                    TimelineScreen(
                        paddingValues = innerPadding,
                        albumId = myImages.id,
                        albumName = myImages.label,
                        handler = myImagesVm.handler,
                        mediaState = myImagesMediaState,
                        albumsState = albumsState,
                        selectionState = myImagesVm.multiSelectState,
                        selectedMedia = myImagesVm.selectedPhotoState,
                        allowNavBar = false,
                        allowHeaders = !hideTimeline,
                        enableStickyHeaders = !hideTimeline,
                        toggleSelection = myImagesVm::toggleSelection,
                        activity = activity,
                        navigate = {
                            navController.navigate(it) {
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
                        onCompressClick = {
                            homeScreenViewModel.handlePickedImages(it, context) {

                            }
                        },
                        onMediaClick = {
                            homeScreenViewModel.handlePickedImages(listOf(it.uri), context) {


                            }
                        }
                    )

                }
            }


            composable(ImageDetailScreen.route) {
                homeScreenViewModel.selectedItem?.let {
                    ImageDetailsScreen(it)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Settings Screen")
    }
}


/*@Composable
internal fun ImageComparisonGrid(imagePairs: List<ImagePair>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(imagePairs) { pair ->
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly // Distribute space evenly
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally // Center items horizontally
                ) {
                    Text(
                        "Compressed",
                        textAlign = TextAlign.Center
                    )
                    AsyncImage(
                        model = pair.compressedUri,
                        contentDescription = "Compressed Image",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .fillMaxWidth()
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally // Center items horizontally
                ) {
                    Text(
                        "Original",
                        textAlign = TextAlign.Center
                    )
                    AsyncImage(
                        model = pair.originalUri,
                        contentDescription = "Original Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

            }
        }
    }
}*/

fun getStoragePermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

// Placeholder functions (replace with your actual image processing logic)


fun compressAndSaveImages(imageUris: List<Uri>, context: Context): List<Uri> {
    val compressedUris = mutableListOf<Uri>()
    for (uri in imageUris) {
        try {
            val bitmap = getBitmapFromUri(uri, context)
            bitmap?.let {
                val compressedUri = compressAndSaveImage(it, context)
                compressedUri?.let {
                    compressedUris.add(it)
                }
            }
        } catch (e: Exception) {
            Log.e("ImageCompression", "Error compressing or saving image", e)
        }
    }
    return compressedUris
}

fun getBitmapFromUri(uri: Uri, context: Context): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        Log.e("ImageCompression", "Error getting bitmap from URI", e)
        null
    }
}

fun compressAndSaveImage(bitmap: Bitmap, context: Context): Uri? {
    val quality = 50 // Adjust quality (0 - 100)
    val outputStream: OutputStream?
    var imageUriResult: Uri? = null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "compressed_image_${System.currentTimeMillis()}.jpg"
            )
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + File.separator + "CompressedImages"
            )
        }
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        outputStream = imageUri?.let { resolver.openOutputStream(it) }
        imageUriResult = imageUri
    } else {
        val imagesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + File.separator + "CompressedImages")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val image = File(imagesDir, "compressed_image_${System.currentTimeMillis()}.jpg")
        outputStream = FileOutputStream(image)
        imageUriResult = Uri.fromFile(image)
    }

    outputStream?.use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
        Log.d("ImageCompression", "Image compressed and saved")
    }
    return imageUriResult

}
package com.image.resizer.compose

import android.Manifest
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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.image.resizer.compose.theme.AppTheme
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

// Data class to hold original and compressed image URIs
data class ImagePair(val originalUri: Uri, val compressedUri: Uri)

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
        Screen.MyImages,
        Screen.Settings
    )
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
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

@Composable
fun Navigation(navController: NavHostController, innerPadding: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.MyImages.route) {
            MyImagesScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}


fun getTotalCompressedImagesCount1(context: Context): Int {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("compressed_image_%")

    val query = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )

    query?.use { cursor ->
        return cursor.count
    }
    return 0
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


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}




@Composable
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
                            .size(150.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
                    )
                }
                Column(
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
                            .size(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

            }
        }
    }
}

fun getStoragePermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

// Placeholder functions (replace with your actual image processing logic)

fun cropImage(imageUris: List<Uri>) {
    // Implement image cropping logic here
    println("Cropping images: $imageUris")
    // Example of iterating uris:
    for (uri in imageUris) {
        println(uri)
    }
}

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
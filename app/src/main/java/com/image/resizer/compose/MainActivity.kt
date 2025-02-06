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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.image.resizer.compose.theme.GeminiTest2Theme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
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
            GeminiTest2Theme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedImageUris by remember { mutableStateOf(emptyList<Uri>()) }
    val context = LocalContext.current
    var imagePairs by remember { mutableStateOf(listOf<ImagePair>()) }
    val viewModel = ScaleImageViewModel()

    // State to control the popup's visibility
    var showScalePopup by remember { mutableStateOf(true) }

    // State to pass to the popup

    Scaffold(
        topBar = {
            MyTopAppBar(
                selectedImageUris,
                context,
                viewModel,
                onShowScalePopup = {
                    showScalePopup = !showScalePopup
                } // Update state on menu click
            ) { compressedUris ->
                imagePairs = selectedImageUris.zip(compressedUris) { original, compressed ->
                    ImagePair(original, compressed)
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (imagePairs.isNotEmpty()) {
                    ImageComparisonGrid(imagePairs)
                } else if (selectedImageUris.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(selectedImageUris) { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(2.dp, Color.Gray, RoundedCornerShape(8.dp))

                            )
                        }
                    }
                }
                GalleryApp(viewModel = viewModel, onImagesSelected = { uris ->
                    selectedImageUris = uris
                })

            }

        }
    }

    // Conditionally display the popup
    if (showScalePopup) {
        val originalDimensions = listOf(Pair(1000, 2000))
        // Implement image scaling logic here
        AnimatedVisibility(
            visible = showScalePopup,
            enter = fadeIn(animationSpec = tween(durationMillis = 20000)) + expandVertically(expandFrom = Alignment.CenterVertically, animationSpec = tween(durationMillis = 13000)),
            exit = fadeOut(animationSpec = tween(durationMillis = 20000)) + shrinkVertically(shrinkTowards = Alignment.CenterVertically, animationSpec = tween(durationMillis = 13000)),
        ) {
            ScaleImagePopup(showScalePopup, onDismiss = {
                showScalePopup = false
            }, originalDimensions, viewModel = viewModel, onScale = {
                it.forEachIndexed { index, it ->
                    System.out.println(" $it here  ${originalDimensions[index]} ")
                }


            })
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopAppBar(
    selectedImageUris: List<Uri>,
    context: Context,
    viewModel: ScaleImageViewModel,
    onShowScalePopup: () -> Unit, // Callback to trigger the popup
    onCompress: (List<Uri>) -> Unit
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Text(
                    text = "Gallery App",
                    modifier = Modifier
                        .weight(1f),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        val compressedUris = compressAndSaveImages(selectedImageUris, context)
                        onCompress(compressedUris)
                    }) {
                        Icon(
                            painterResource(id = R.drawable.ic_compress_24dp),
                            contentDescription = "Compress",
                        )
                    }
                    IconButton(onClick = {
                        onShowScalePopup() // Trigger the popup via the callback
                    }) {
                        Icon(
                            painterResource(id = R.drawable.ic_compress_24dp),
                            contentDescription = "Scale",
                        )
                    }
                    IconButton(onClick = { cropImage(selectedImageUris) }) {
                        Icon(
                            painterResource(id = R.drawable.ic_compress_24dp),
                            contentDescription = "Crop",
                        )
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryApp(onImagesSelected: (List<Uri>) -> Unit, viewModel: ScaleImageViewModel) {
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            onImagesSelected(uris)
        }
    )
    val galleryPermissionState = rememberPermissionState(
        getStoragePermission()
    )
    var showRationale by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(true) }
    var onDismiss by remember {
        mutableStateOf({
            showDialog = false
        })
    }
    val originalDimensions = listOf(Pair(1000, 2000))


    Column(
        modifier = Modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                showDialog = true

                 if (galleryPermissionState.status.isGranted) {
                       multiplePhotoPickerLauncher.launch(
                           PickVisualMediaRequest(
                               ActivityResultContracts.PickVisualMedia.ImageOnly
                           )
                       )
                   } else {
                       showRationale = true
                   }

            }
        ) {
            Text("Select Images")
        }


    }
    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                showRationale = false
            },
            title = { Text("Permission Required") },
            text = { Text("The app needs permission to access your gallery.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        galleryPermissionState.launchPermissionRequest()
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun ImageComparisonGrid(imagePairs: List<ImagePair>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        contentPadding = PaddingValues(16.dp),
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
                        textAlign = TextAlign.Center,
                        modifier = Modifier.height(80.dp)
                    )
                    AsyncImage(
                        model = pair.compressedUri,
                        contentDescription = "Compressed Image",
                        modifier = Modifier
                            .size(150.dp)

                            .clip(RoundedCornerShape(8.dp))
                            .border(12.dp, Color.Red, RoundedCornerShape(8.dp))
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally // Center items horizontally
                ) {
                    Text(
                        "Original",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.height(80.dp)
                    )
                    AsyncImage(
                        model = pair.originalUri,
                        contentDescription = "Original Image",
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, Color.Gray, RoundedCornerShape(8.dp))
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
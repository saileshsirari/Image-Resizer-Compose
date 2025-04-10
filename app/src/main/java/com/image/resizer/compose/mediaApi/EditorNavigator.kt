package com.image.resizer.compose.mediaApi

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.image.resizer.compose.CompressState
import com.image.resizer.compose.CropState
import com.image.resizer.compose.GalleryState
import com.image.resizer.compose.HomeScreenViewModel
import com.image.resizer.compose.ScaleState

@Composable
fun EditorNavigator(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    targetImage: Bitmap?,
    targetUri: Uri?,
    startCropping: () -> Unit = {},
    isSupportingPanel: Boolean = false,
    homeScreenViewModel: HomeScreenViewModel,
    onItemClick: (EditorDestination) -> Unit = {},
) {


    EditorSelector(
        isSupportingPanel = isSupportingPanel,
        homeScreenViewModel = homeScreenViewModel,
        onItemClick = { editorItem ->
            val dest =  when (editorItem) {
                EditorItems.Scale -> EditorDestination.Scale
                EditorItems.Crop -> EditorDestination.Crop
                EditorItems.Compress -> EditorDestination.Compress
                EditorItems.Undo -> EditorDestination.Undo
                EditorItems.Save -> EditorDestination.Save
                EditorItems.Replace -> EditorDestination.Replace
            }
            onItemClick(dest)
        }
    )
}


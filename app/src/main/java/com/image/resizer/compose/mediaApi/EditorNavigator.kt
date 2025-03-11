package com.image.resizer.compose.mediaApi

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@Composable
fun EditorNavigator(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    targetImage: Bitmap?,
    targetUri: Uri?,
    startCropping: () -> Unit = {},
    isSupportingPanel: Boolean = false,
    onItemClick: (EditorDestination) -> Unit = {}
) {


            EditorSelector(
                isSupportingPanel = isSupportingPanel,
                onItemClick = { editorItem ->
                    val dest = when (editorItem) {
                        EditorItems.Scale ->   EditorDestination.Scale
                        EditorItems.Crop -> EditorDestination.Crop
                        EditorItems.Compress ->   EditorDestination.Compress
                        EditorItems.Undo ->   EditorDestination.Undo
                    }
                    onItemClick(dest)
                }
            )
}


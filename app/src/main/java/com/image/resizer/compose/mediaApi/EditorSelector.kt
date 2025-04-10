package com.image.resizer.compose.mediaApi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.image.resizer.compose.CompressState
import com.image.resizer.compose.CropState
import com.image.resizer.compose.HomeScreenViewModel
import com.image.resizer.compose.ScaleState

@Composable
fun EditorSelector(
    modifier: Modifier = Modifier,
    isSupportingPanel: Boolean,
    homeScreenViewModel: HomeScreenViewModel,
    onItemClick: (EditorItems) -> Unit = {}
) {
    val cropState by homeScreenViewModel.cropState.collectAsState()
    val compressState by homeScreenViewModel.compressState.collectAsState()
    val scaleState by homeScreenViewModel.scaleState.collectAsState()

    val imagesTransformed by remember {
        derivedStateOf {
            cropState is CropState.Success || scaleState is ScaleState.Success ||
                    compressState is CompressState.Success
        }
    }
    val padding = remember(isSupportingPanel) {
        if (isSupportingPanel) PaddingValues(0.dp) else PaddingValues(horizontal = 16.dp, vertical = 2.dp)
    }

    SupportiveLazyLayout(
        modifier = modifier
            .then(
                if (isSupportingPanel) Modifier
                    .safeSystemGesturesPadding(onlyRight = true)
                    .clipToBounds()
                    .clip(RoundedCornerShape(16.dp))
                else Modifier.animateContentSize()
            ),
        isSupportingPanel = isSupportingPanel,
        contentPadding = padding
    ) {
        itemsIndexed(
            items = EditorItems.entries,
            key = { _, it -> it.name }
        ) { index, editorItem ->
            if(editorItem == EditorItems.Undo || editorItem == EditorItems.Replace
                || editorItem == EditorItems.Save){
                EditorItem(
                    enabled =   imagesTransformed,
                    imageVector = editorItem.icon,
                    title = editorItem.translatedName,
                    horizontal = isSupportingPanel,
                    onItemClick = {
                        onItemClick(editorItem)
                    }
                )
            }else {
                EditorItem(
                    enabled =   !imagesTransformed,
                    imageVector = editorItem.icon,
                    title = editorItem.translatedName,
                    horizontal = isSupportingPanel,
                    onItemClick = {
                        onItemClick(editorItem)
                    }
                )
            }

            if (isSupportingPanel && index < EditorItems.entries.size - 1) {
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }
}
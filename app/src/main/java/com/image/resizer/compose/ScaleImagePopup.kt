package com.image.resizer.compose

import android.R.attr.label
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.image.resizer.compose.mediaApi.MediaHandleUseCase
import com.image.resizer.compose.mediaApi.MediaRepositoryImpl
import com.image.resizer.compose.theme.MyTypography
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleImagePopup(
    onDismiss: () -> Unit,
    homeScreenViewModel: HomeScreenViewModel,
    onScale: (ScaleParams?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScaleImageViewModel
) {
    //val imageItems = homeScreenViewModel.selectedImageItems.collectAsState()
 //   val originalDimensions =
   //     imageItems.value.map { it.imageDimension }

    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Custom", "Percentage")
   // viewModel.setOriginalDimensions(originalDimensions)
    var isButtonEnabled by remember { mutableStateOf(false) }
    var hasPredefinedSelection by remember { mutableStateOf(false) }
// Calculate fixed height for the content
    val fixedContentHeight = 250.dp // Adjust this value as needed

    fun updateButtonEnableState() {
        isButtonEnabled = if (viewModel.mode == "custom") {
            ((!viewModel.keepAspectRatio &&
                    viewModel.width.isNotEmpty() && viewModel.height.isNotEmpty())
                    || (viewModel.keepAspectRatio &&
                    (viewModel.width.isNotEmpty() || viewModel.height.isNotEmpty())) || hasPredefinedSelection)
        } else {
            true
        }
    }

    LaunchedEffect(
        viewModel.width,
        viewModel.height,
        hasPredefinedSelection,
        viewModel.keepAspectRatio,
        viewModel.mode
    ) {
        updateButtonEnableState()
    }

    AlertDialog(
        onDismissRequest = { onDismiss() }, // Dismiss on outside click
        title = { Text("Scale Image") },
        modifier = modifier,
        text = {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                // Removed TabRow and HorizontalDivider
                // Added CustomScaleTabContent directly
                CustomScaleTabContent(
                    viewModel,
                    onPredefinedSelect = { hasPredefinedSelection = it },
                )

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val scaleParam  =  viewModel.onScaleForList()
                    onScale(scaleParam)
                },
                enabled = isButtonEnabled
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomScaleTabContent(viewModel: ScaleImageViewModel, onPredefinedSelect: (Boolean) -> Unit) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val maxChar = 4
    Column(Modifier.padding(16.dp)) {
        // Dropdown for Predefined Dimensions
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(

                value = viewModel.selectedPredefinedDimension.let {
                    if (it.width == -1 && it.height == -1) {
                        "Select"
                    } else {
                        it.toString()
                    }
                },
                onValueChange = {

                },
                readOnly = true,
                label = { Text("Select Dimension", style = MyTypography.titleMedium) },
                trailingIcon = {
                    IconButton( onClick = { isDropdownExpanded = true }) {
                        Icon(
                            painterResource(id = R.drawable.ic_compress_24dp),
                            contentDescription = "Dropdown"
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            )

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Select", style = MyTypography.titleMedium) },
                    onClick = {
                        viewModel.resetSelectedPredefinedDimension()
                        onPredefinedSelect(false)
                        isDropdownExpanded = false
                    })
                viewModel.predefinedDimensions.forEachIndexed { index, dimension ->
                    DropdownMenuItem(
                        text = { Text(dimension.toString(),
                            style = MyTypography.bodyMedium) },
                        onClick = {
                            viewModel.selectPredefinedDimension(dimension, index)
                            onPredefinedSelect(true)
                            isDropdownExpanded = false
                            viewModel.updateHeight("")
                            viewModel.updateWidth("")
                        })
                }
            }
        }
        Spacer(modifier = Modifier.padding(8.dp))

        // Custom Width and Height fields
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(

                value = viewModel.width,
                onValueChange = {
                    if (it.length <= maxChar) {
                        viewModel.updateWidth(it)
                        viewModel.resetSelectedPredefinedDimension()
                        onPredefinedSelect(false)
                        isDropdownExpanded = false
                    }
                },
                label = { Text("Width",style = MyTypography.titleMedium) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = viewModel.height,

                onValueChange = {
                    if (it.length <= maxChar) {
                        viewModel.updateHeight(it)
                        viewModel.resetSelectedPredefinedDimension()
                        onPredefinedSelect(false)
                        isDropdownExpanded = false
                    }
                },
                label = { Text("Height",  style = MyTypography.titleMedium) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.padding(8.dp))

        // Aspect Ratio Checkbox
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = viewModel.keepAspectRatio,
                onCheckedChange = {
                    viewModel.toggleKeepAspectRatio(it)
                }
            )
            Text("Keep Aspect Ratio")
        }
        Spacer(modifier = Modifier.padding(8.dp))

    }
}

@Preview
@Composable
fun CustomScaleTabContentPreview(){
    val viewModel = ScaleImageViewModel()
    CustomScaleTabContent(viewModel) {

    }
}

@Composable
fun PercentageScaleTabContent(viewModel: ScaleImageViewModel) {
    val animatedAlpha by animateFloatAsState(
        targetValue = viewModel.percentage,
        label = "alpha"
    )
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Percentage: ${viewModel.percentage.roundToInt()}%")
        }
        Slider(
            value = viewModel.percentage,
            onValueChange = { viewModel.updatePercentage(it) },
            valueRange = 10f..100f,
            steps = 89,

            )
    }
}

@Preview(showBackground = true)
@Composable
fun ScaleImagePopupPreview() {
    var showDialog by remember { mutableStateOf(true) }
    var onDismiss by remember { mutableStateOf({}) }
    val mediaRepository = MediaRepositoryImpl(LocalContext.current)
    val mediaHandleUseCase = MediaHandleUseCase(mediaRepository)
    val homeScreenViewModel = HomeScreenViewModel(mediaHandleUseCase)
    val originalDimensions = listOf(Pair(1000, 2000))
    ScaleImagePopup(
        onDismiss,
        homeScreenViewModel,
        viewModel = ScaleImageViewModel(),
        onScale = {

        })
}

@Preview
@Composable
fun test() {


}


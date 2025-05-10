package com.image.resizer.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import apps.sai.com.imageresizer.R
import com.image.resizer.compose.mediaApi.MediaHandleUseCase
import com.image.resizer.compose.mediaApi.MediaRepositoryImpl
import com.image.resizer.compose.theme.MyTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleImagePopup(
    onDismiss: () -> Unit,
    onScale: (ScaleParams?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScaleImageViewModel
) {

    var isButtonEnabled by remember { mutableStateOf(false) }
    var hasPredefinedSelection by remember { mutableStateOf(false) }
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
                            viewModel.selectPredefinedDimension(dimension)
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
                        // filter the input
                    onTextEntered(it, maxChar, viewModel){
                        viewModel.updateWidth(it)
                    }
                    onPredefinedSelect(false)
                    isDropdownExpanded = false
                },
                label = { Text("Width",style = MyTypography.titleMedium) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = viewModel.height,

                onValueChange = {
                    onTextEntered(it, maxChar, viewModel){
                        viewModel.updateHeight(it)
                    }
                    onPredefinedSelect(false)
                    isDropdownExpanded = false
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

private fun onTextEntered(
    string: String,
    maxChar: Int,
    viewModel: ScaleImageViewModel,
    onValueEntered: (String) -> Unit = {}
) {
    val filteredValue = string.filter { char -> char.isDigit() }
    if (filteredValue.length <= maxChar) {
        viewModel.resetSelectedPredefinedDimension()
        onValueEntered(filteredValue)
    }

}

@Preview
@Composable
fun CustomScaleTabContentPreview(){
    val viewModel = ScaleImageViewModel()
    CustomScaleTabContent(viewModel) {

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
        viewModel = ScaleImageViewModel(),
        onScale = {

        })
}

@Preview
@Composable
fun test() {


}


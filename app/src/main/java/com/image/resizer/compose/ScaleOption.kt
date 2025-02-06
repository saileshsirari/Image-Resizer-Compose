package com.image.resizer.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleImagePopup(
    show: Boolean,
    onDismiss: () -> Unit,
    originalDimensions: List<Pair<Int, Int>>,
    onScale: (List<ScaleParams>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScaleImageViewModel
) {

        if (show) {
            var tabIndex by remember { mutableIntStateOf(0) }
            val tabs = listOf("Custom", "Percentage")
            viewModel.setOriginalDimensions(originalDimensions)
            var isButtonEnabled by remember { mutableStateOf(false) }
            var hasPredefinedSelection by remember { mutableStateOf(false) }
// Calculate fixed height for the content
            val fixedContentHeight = 250.dp // Adjust this value as needed

            fun updateButtonEnableState() {
                if (viewModel.mode == "custom") {
                    isButtonEnabled =
                        (viewModel.width.isNotEmpty() && viewModel.height.isNotEmpty()) || hasPredefinedSelection
                } else {
                    isButtonEnabled = true
                }
            }

            LaunchedEffect(
                viewModel.width,
                viewModel.height,
                hasPredefinedSelection,
                viewModel.mode
            ) {
                updateButtonEnableState()
            }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Scale Image") },
                modifier = modifier,
                text = {

                    Column(
                        modifier = Modifier
                            .wrapContentHeight()
                            .fillMaxWidth()
                    ) {
                        TabRow(selectedTabIndex = tabIndex) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    text = { Text(title) },
                                    selected = tabIndex == index,
                                    onClick = {
                                        tabIndex = index
                                        viewModel.changeMode(if (index == 0) "custom" else "percentage")
                                        hasPredefinedSelection = false
                                    }
                                )
                            }
                        }
                        HorizontalDivider()
                        // Use a fixed height container
                        Column(
                            modifier = Modifier
                                .height(fixedContentHeight) // Fixed height!
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (tabIndex) {
                                0 -> CustomScaleTabContent(
                                    viewModel,
                                    onPredefinedSelect = { hasPredefinedSelection = it })

                                1 -> PercentageScaleTabContent(viewModel)
                            }
                        }

                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.onScaleForList(onScale)
                        onDismiss()

                    }, enabled = isButtonEnabled) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                },
//            properties = PopupProperties(focusable = true)
            )
        }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomScaleTabContent(viewModel: ScaleImageViewModel, onPredefinedSelect: (Boolean) -> Unit) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
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
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Dimension") },
                trailingIcon = {
                    IconButton(onClick = { isDropdownExpanded = true }) {
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
                    text = { Text("Select") },
                    onClick = {
                        viewModel.resetSelectedPredefinedDimension()
//                        viewModel.updateHeight("")
//                        viewModel.updateWidth("")
                        onPredefinedSelect(false)
                        isDropdownExpanded = false
                    })
                viewModel.predefinedDimensions.forEachIndexed { index, dimension ->
                    DropdownMenuItem(
                        text = { Text(dimension.toString()) },
                        onClick = {
                            viewModel.selectPredefinedDimension(dimension, index)
                            onPredefinedSelect(true)
                            isDropdownExpanded = false
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
                    viewModel.updateWidth(it)
                    onPredefinedSelect(false)
                },
                label = { Text("Width") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = viewModel.height,
                onValueChange = {
                    viewModel.updateHeight(it)

                    onPredefinedSelect(false)
                },
                label = { Text("Height") },
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
            valueRange = 1f..200f,
            steps = 199,

        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScaleImagePopupPreview() {
    var showDialog by remember { mutableStateOf(true) }
    var onDismiss by remember { mutableStateOf({}) }
    val originalDimensions = listOf(Pair(1000, 2000))
    ScaleImagePopup(
        showDialog,
        onDismiss,
        originalDimensions,
        viewModel = ScaleImageViewModel(),
        onScale = {

        })
}

@Preview
@Composable
fun test() {


}


package com.image.resizer.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.image.resizer.compose.theme.MyTypography

@Composable
fun LinearProgressIndicatorWithPercentage(
    progress: Float,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged {
                size = it
            }
    ) {

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            trackColor = trackColor,
        )
        if (showPercentage) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MyTypography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.CenterEnd),
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
    }
}
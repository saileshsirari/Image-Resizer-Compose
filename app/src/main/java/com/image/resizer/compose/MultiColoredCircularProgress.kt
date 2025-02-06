package com.image.resizer.compose


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas // This is the correct import
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun MultiColoredCircularProgress(
    progress: Float, // 0.0 to 1.0
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 12f,
    circleBackgroundColor: Color = Color.LightGray
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val center = Offset(x = canvasWidth / 2, y = canvasHeight / 2)
        val radius = (canvasWidth - strokeWidth) / 2

        // Draw background circle
        drawCircle(
            color = circleBackgroundColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        val sweepAngle = 360f * progress

        // Draw the multi-colored progress
        var startAngle = -90f // Start from the top
        val segmentCount = colors.size
        val segmentAngle = sweepAngle / segmentCount

        for (i in 0 until segmentCount) {
            val color = colors[i]
            val endAngle = startAngle + segmentAngle
            val drawAngle = if (endAngle - startAngle > 0) endAngle - startAngle else 0f
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = drawAngle,
                useCenter = false,
                topLeft = Offset(
                    x = center.x - radius,
                    y = center.y - radius
                ),
                size = Size(width = radius * 2, height = radius * 2),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
            startAngle = endAngle
        }
    }
}



@Composable
fun AnimatedMultiColoredCircularProgress(
    targetProgress: Float,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 42f,
    circleBackgroundColor: Color = Color.Transparent,
    animationDurationMillis: Int = 1000, // Duration of the animation in milliseconds
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(targetProgress) {
        progress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = animationDurationMillis, easing = FastOutLinearInEasing)
        )
    }
    MultiColoredCircularProgress(
        progress = progress.value,
        colors = colors,
        modifier = modifier,
        strokeWidth = strokeWidth,
        circleBackgroundColor = circleBackgroundColor
    )
}
@Composable
fun InfiniteAnimatedMultiColoredCircularProgress(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 42f,
    circleBackgroundColor: Color = Color.LightGray,
    animationDurationMillis: Int = 2000, // Duration of the animation in milliseconds
    easing: Easing = FastOutSlowInEasing
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = animationDurationMillis,easing = easing),
                repeatMode = RepeatMode.Restart
            )
        )
    }
    MultiColoredCircularProgress(
        progress = progress.value,
        colors = colors,
        modifier = modifier,
        strokeWidth = strokeWidth,
        circleBackgroundColor = circleBackgroundColor
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewInfiniteAnimatedMultiColoredCircularProgress() {
    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center){
        InfiniteAnimatedMultiColoredCircularProgress(
            modifier = Modifier.size(150.dp),
            colors = listOf( colorResource(id = R.color.red_100),
                colorResource(id = R.color.violet_100), colorResource(id = R.color.yellow_100)),
        )
    }

}

@Preview(showBackground = true)
@Composable
fun PreviewAnimatedMultiColoredCircularProgress() {
    var currentProgress by remember { mutableStateOf(0.3f) }
    Column {
        AnimatedMultiColoredCircularProgress(
            targetProgress = currentProgress,
            listOf( colorResource(id = R.color.red_100),
                colorResource(id = R.color.violet_100), colorResource(id = R.color.yellow_100)),
            modifier = Modifier.size(150.dp)
        )
        Button(onClick = {
            currentProgress = (0..10).random()/10f
        }){
            Text("Change Progress")
        }
    }
   /* Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        AnimatedMultiColoredCircularProgress(
            targetProgress = 0.7f,
            colors = listOf(Color.Red, Color.Green, Color.Blue),
        )
    }*/

}
//@Preview(showBackground = true)
@Composable
fun PreviewMultiColoredCircularProgress() {
    MultiColoredCircularProgress(
        progress = 1f,
        colors =
            listOf( colorResource(id = R.color.red_100),
                colorResource(id = R.color.violet_100), colorResource(id = R.color.yellow_100)),
        modifier = Modifier.size(180.dp),
        strokeWidth = 25f // Set the stroke width to 25dp
    )

}


@Preview(showBackground = true)
@Composable
fun PreviewInfiniteAnimatedMultiColoredCircularProgressLinearEasing() {
    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center){
        InfiniteAnimatedMultiColoredCircularProgress(
            modifier = Modifier.size(150.dp),
            colors = listOf( colorResource(id = R.color.red_100),
                colorResource(id = R.color.violet_100), colorResource(id = R.color.yellow_100)),
            easing = LinearEasing
        )
    }

}

@Preview(showBackground = true)
@Composable
fun PreviewInfiniteAnimatedMultiColoredCircularProgressFastOutLinearInEasing() {
    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center){
        InfiniteAnimatedMultiColoredCircularProgress(
            modifier = Modifier.size(150.dp),
            colors = listOf( colorResource(id = R.color.red_100),
                colorResource(id = R.color.violet_100), colorResource(id = R.color.yellow_100)),
            easing = FastOutLinearInEasing
        )
    }

}

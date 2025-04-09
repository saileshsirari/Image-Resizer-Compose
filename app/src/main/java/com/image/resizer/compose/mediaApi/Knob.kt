package com.image.resizer.compose.mediaApi

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun RangeSlider(
    range: ClosedFloatingPointRange<Float>,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    rangeStart: Int,
    rangeEnd: Int,
    maxRange: Int
) {
    var dragStarted by remember { mutableStateOf(false) }
    var selectedKnob by remember { mutableStateOf<Knob?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val minValuePx = 0f
        val maxValuePx = maxWidthPx
        val density = LocalDensity.current.density
        val knobRadiusPx = (10.dp * density).value

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = rangeStart.toString())
                Spacer(modifier = Modifier.weight(1f))
                Text(text = rangeEnd.toString())
            }
            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStarted = true
                                val startKnobX = (range.start / maxRange) * maxValuePx
                                val endKnobX = (range.endInclusive / maxRange) * maxValuePx

                                val startKnobOffset = Offset(startKnobX, size.height / 2f)
                                val endKnobOffset = Offset(endKnobX, size.height / 2f)

                                if (isPointInCircle(offset, startKnobOffset, knobRadiusPx)) {
                                    selectedKnob = Knob.START
                                } else if (isPointInCircle(offset, endKnobOffset, knobRadiusPx)) {
                                    selectedKnob = Knob.END
                                }
                            },
                            onDrag = { change, dragAmount ->
                                if (dragStarted && selectedKnob != null) {
                                    change.consume()
                                    val newX = when (selectedKnob) {
                                        Knob.START -> {
                                            (range.start / maxRange) * maxValuePx + dragAmount.x
                                        }

                                        Knob.END -> {
                                            (range.endInclusive / maxRange) * maxValuePx + dragAmount.x
                                        }

                                        null -> 0f
                                    }

                                    var newStart = range.start
                                    var newEnd = range.endInclusive

                                    when (selectedKnob) {
                                        Knob.START -> {
                                            newStart = (newX.coerceIn(
                                                minValuePx,
                                                maxValuePx
                                            ) / maxValuePx) * maxRange
                                        }

                                        Knob.END -> {
                                            newEnd = (newX.coerceIn(
                                                minValuePx,
                                                maxValuePx
                                            ) / maxValuePx) * maxRange
                                        }

                                        null -> {}
                                    }

                                    if (newStart <= newEnd) {
                                        onRangeChange(newStart..newEnd)
                                    } else {
                                        val temp = newStart
                                        newStart = newEnd
                                        newEnd = temp
                                        onRangeChange(newStart..newEnd)
                                    }
                                }
                            },
                            onDragEnd = {
                                dragStarted = false
                                selectedKnob = null
                            },
                        )
                    }) {
                val center = Offset(size.width / 2, size.height / 2)
                val startKnobX = (range.start / maxRange) * maxValuePx
                val endKnobX = (range.endInclusive / maxRange) * maxValuePx
                val startKnob = Offset(startKnobX, size.height / 2f)
                val endKnob = Offset(endKnobX, size.height / 2f)
                val barWidth = 5.dp.toPx()
                val knobRadius = 10.dp.toPx()

                drawRoundRect(
                    color = Color.LightGray,
                    topLeft = Offset(0f, center.y - barWidth / 2),
                    size = Size(size.width, barWidth),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )

                drawRoundRect(
                    color = Color.Blue,
                    topLeft = Offset(startKnobX, center.y - barWidth / 2),
                    size = Size(endKnobX - startKnobX, barWidth),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )

                drawCircle(
                    color = Color.DarkGray,
                    center = startKnob,
                    radius = knobRadius
                )
                drawCircle(
                    color = Color.DarkGray,
                    center = endKnob,
                    radius = knobRadius
                )
            }
        }
    }
}

enum class Knob {
    START, END
}

fun isPointInCircle(point: Offset, circleCenter: Offset, circleRadius: Float): Boolean {
    val distance = (point - circleCenter).getDistance()
    return distance <= circleRadius
}
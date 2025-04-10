package com.image.resizer.compose

import android.net.Uri
import android.util.Log.i
import androidx.activity.result.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class ScaleOption(val label: String, val scaleFactor: Float? = null)
data class PredefinedDimension(val width: Int, val height: Int) {
    override fun toString(): String {
        return "${width}x${height}"
    }
}


data class ScaleParams(
    val newWidth: Int?=null,
    val newHeight: Int?=null,
    val scaleFactor: Float? = null,
    val keepAspectRatio: Boolean = true,
    val compressPercentage: Int? = null
)

class ScaleImageViewModel : ViewModel() {
    var mode by mutableStateOf("custom")
        private set
    var width by mutableStateOf("")
        private set
    var height by mutableStateOf("")
        private set
    var keepAspectRatio by mutableStateOf(true)
        private set
    var expanded by mutableStateOf(false)
        private set
    var percentage by mutableStateOf(100f)
        private set
    var aspectRatio = mutableListOf<Float>()
        private set
    val predefinedDimensions = listOf(
        PredefinedDimension(320, 240),
        PredefinedDimension(640, 480),
        PredefinedDimension(800, 600),
        PredefinedDimension(1024, 768),
        PredefinedDimension(1280, 1024)
    )
    var selectedPredefinedDimension by mutableStateOf<PredefinedDimension>(
        PredefinedDimension(
            -1,
            -1
        )
    )
        private set
    var originalDimensions = mutableListOf<Pair<Int, Int>>()
        private set

    fun setOriginalDimensions(originalDimensions: List<Pair<Int, Int>>) {

        this.originalDimensions.clear()
        this.originalDimensions.addAll(originalDimensions)

    }

    fun changeMode(newMode: String) {
        mode = newMode
    }

    fun updateWidth(newWidth: String) {
        width = newWidth
        /*  if (keepAspectRatio) {
              updateHeightBasedOnWidth()
          }*/
    }

    fun updateHeight(newHeight: String) {
        height = newHeight
        /* if (keepAspectRatio) {
             updateWidthBasedOnHeight()
         }*/
    }

    fun toggleKeepAspectRatio(newKeepAspectRatio: Boolean) {
        keepAspectRatio = newKeepAspectRatio
    }

    fun selectPredefinedDimension(dimension: PredefinedDimension, index: Int) {
        selectedPredefinedDimension = dimension
        if (selectedPredefinedDimension.width != -1 && selectedPredefinedDimension.height != -1) {
            //  width = selectedPredefinedDimension.width.toString()
            //   height = selectedPredefinedDimension.height.toString()
        }
        /* if (keepAspectRatio) {
             if (aspectRatio[0] > 1) {
                 width = selectedPredefinedDimension.width.toString()
                 height =
                     (selectedPredefinedDimension.width / aspectRatio[0]).roundToInt().toString()
             } else {
                 height = selectedPredefinedDimension.height.toString()
                 width =
                     (selectedPredefinedDimension.height * aspectRatio[0]).roundToInt().toString()
             }
         } else {
             width = selectedPredefinedDimension.width.toString()
             height = selectedPredefinedDimension.height.toString()
         }*/
    }


    fun updatePercentage(newPercentage: Float) {
        percentage = newPercentage
    }


    fun onScaleForList(): ScaleParams? {
        var result :ScaleParams?=null

        if (mode == "custom") {
            if (selectedPredefinedDimension.width != -1 && selectedPredefinedDimension.height != -1) {
                width = selectedPredefinedDimension.width.toString()
                height = selectedPredefinedDimension.height.toString()
                result =  ScaleParams(
                    selectedPredefinedDimension.width,
                    selectedPredefinedDimension.height,
                    null,
                    keepAspectRatio)
                return  result
            }

            if (keepAspectRatio) {
                if (width.isNotEmpty()) {
                    val newWidth = width.toInt()
                    result =    ScaleParams(
                        newWidth,
                        null,
                        null,
                        keepAspectRatio
                    )

                } else if (height.isNotEmpty()) {
                    val newHeight = height.toInt()
                    result =   ScaleParams(
                        null,
                        newHeight,
                        null,
                        keepAspectRatio
                    )

                }
            }
            if (!keepAspectRatio && width.isNotEmpty() && height.isNotEmpty()) {
                val newWidth = width.toInt()
                val newHeight = height.toInt()
                result =   ScaleParams(
                    newWidth,
                    newHeight,
                    null,
                    keepAspectRatio
                )

            }

        } else {
            result =  ScaleParams(
                null,
                null,
                percentage / 100f,
                keepAspectRatio
            )

        }
      return  result
    }

    fun resetSelectedPredefinedDimension() {
        selectedPredefinedDimension = PredefinedDimension(-1, -1)
    }


}
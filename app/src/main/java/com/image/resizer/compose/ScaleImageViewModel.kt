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
        PredefinedDimension(144, 176),
        PredefinedDimension(240, 320),
        PredefinedDimension(288, 352),
        PredefinedDimension(480, 640),
        PredefinedDimension(480, 720),
        PredefinedDimension(600, 800),
        PredefinedDimension(720, 1280),
        PredefinedDimension(900, 1600),
        PredefinedDimension(1080, 1920),
        PredefinedDimension(1170, 2080),
        PredefinedDimension(1200, 1600),
        PredefinedDimension(1458, 2592),
        PredefinedDimension(1536, 2048),
        PredefinedDimension(1560, 2080),
        PredefinedDimension(1836, 3264),
        PredefinedDimension(1944, 2592),
        PredefinedDimension(2052, 3648),
        PredefinedDimension(2304, 4096),
        PredefinedDimension(2448, 3264),
        PredefinedDimension(2736, 3648),
        PredefinedDimension(3072, 4096)
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
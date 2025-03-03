package com.image.resizer.compose

import android.net.Uri
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
    val newWidth: Int,
    val newHeight: Int,
    val scaleFactor: Float? =null,
    val keepAspectRatio: Boolean =true
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
        aspectRatio.clear()
        originalDimensions.forEach { (width, height) ->
            aspectRatio.add(width.toFloat() / height.toFloat())
        }
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
        if(selectedPredefinedDimension.width!=-1 && selectedPredefinedDimension.height!=-1){
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

    fun toggleDropdown() {
        expanded = !expanded
    }

    fun updatePercentage(newPercentage: Float) {
        percentage = newPercentage
    }

    private fun updateHeightBasedOnWidth() {
        val newWidth = width.toIntOrNull() ?: 0
        val newHeight = (newWidth / aspectRatio[0]).roundToInt()
        height = newHeight.toString()
    }

    private fun updateWidthBasedOnHeight() {
        val newHeight = height.toIntOrNull() ?: 0
        val newWidth = (newHeight * aspectRatio[0]).roundToInt()
        width = newWidth.toString()
    }

    fun onScaleForList(onResult: (List<ScaleParams>) -> Unit) {
        val resultList = mutableListOf<ScaleParams>()

        if (mode == "custom") {
            if(selectedPredefinedDimension.width!=-1 && selectedPredefinedDimension.height!=-1){
                width = selectedPredefinedDimension.width.toString()
                height = selectedPredefinedDimension.height.toString()
            }
            if(keepAspectRatio ){
                if(width.isNotEmpty()){
                    val newWidth = width.toInt()
                    originalDimensions.forEach { (width, height) ->
                        //original aspect ratio
                        val aspect = width.toFloat() / height.toFloat()
                        resultList.add(
                            ScaleParams(
                                newWidth,
                                (newWidth / aspect).roundToInt(),
                                null,
                                keepAspectRatio
                            )
                        )
                    }
                }else if(height.isNotEmpty()){
                    val newHeight = height.toInt()
                    originalDimensions.forEach { (width, height) ->
                        //original aspect ratio
                        val aspect = width.toFloat() / height.toFloat()
                        resultList.add(
                            ScaleParams(
                                (newHeight* aspect).roundToInt(),
                                newHeight,
                                null,
                                keepAspectRatio
                            )
                        )
                    }
                }
            }
            if(!keepAspectRatio && width.isNotEmpty() && height.isNotEmpty()) {
                val newWidth = width.toInt()
                val newHeight = height.toInt()
                    originalDimensions.forEach { (_, _) ->
                        resultList.add(
                            ScaleParams(
                                newWidth,
                                newHeight,
                                null,
                                keepAspectRatio
                            )
                        )
                    }
            }

        } else {
            for (i in 0 until originalDimensions.size) {
                val scaleFactor = percentage / 100f
                resultList.add(
                    ScaleParams(
                        (originalDimensions[i].first * scaleFactor).roundToInt(),
                        (originalDimensions[i].second * scaleFactor).roundToInt(),
                        scaleFactor,
                        true
                    )
                )
            }
        }
        onResult(resultList)
    }

    fun resetSelectedPredefinedDimension() {
        selectedPredefinedDimension = PredefinedDimension(-1, -1)
    }


}
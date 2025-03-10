@file:Suppress("KotlinConstantConditions")

package com.image.resizer.compose.mediaApi.util

import android.util.Log
import com.github.panpf.sketch.core.BuildConfig

fun printInfo(message: Any) {
    Log.i("GalleryInfo", message.toString())
}

fun printDebug(message: Any) {
    printDebug(message.toString())
}

fun printDebug(message: String) {
  //  if (BuildConfig.BUILD_TYPE != "release") {
        Log.d("GalleryInfo", message)
   // }
}

fun printError(message: String) {
    Log.e("GalleryInfo", message)
}

fun printWarning(message: String) {
    Log.w("GalleryInfo", message)
}
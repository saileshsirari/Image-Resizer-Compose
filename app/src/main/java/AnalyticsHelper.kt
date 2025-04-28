package com.image.resizer.utils

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import android.content.Context
import android.util.StatsLog.logEvent
import androidx.core.os.bundleOf
import com.image.resizer.compose.log
import com.image.resizer.utils.AnalyticsHelper.EVENT_IMAGE_SAVED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun CoroutineScope.logEvent(block: suspend () -> Unit){
    launch(Dispatchers.IO) {
        block()
        log(tag = AnalyticsHelper.TAG, message = "$block")
    }
}
object AnalyticsHelper {
     const val TAG = "AnalyticsHelper"

    private const val EVENT_REVIEW_SHOWN = "review_shown"
    private const val EVENT_IMAGE_REPLACED = "image_replaced"
    private const val EVENT_IMAGE_COMPRESSED = "image_compressed"
    private const val EVENT_IMAGE_SCALED = "image_scaled"
    private const val EVENT_IMAGE_SAVED = "image_scaled"
    private const val EVENT_IMAGES_SELECTED = "images_selected"


    private lateinit var analytics: FirebaseAnalytics


    fun initialize(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)
    }

     fun logImagesSelected(count: Int) {
         logEvent(EVENT_IMAGES_SELECTED, bundleOf(FirebaseAnalytics.Param.VALUE to count))
     }
    fun logReviewShown() {
        logEvent(EVENT_REVIEW_SHOWN)
    }

    fun logImageReplaced() {
        logEvent(EVENT_IMAGE_REPLACED)
    }
    fun logImageSaved() {
        logEvent(EVENT_IMAGE_SAVED)

    }

    fun logImageCompressed() {
        logEvent(EVENT_IMAGE_COMPRESSED)
    }

    fun logImageScaled() {
        logEvent(EVENT_IMAGE_SCALED)
    }

    private fun logEvent(eventName: String, bundle: Bundle? = null) {
        analytics.logEvent(eventName, bundle)
    }
}
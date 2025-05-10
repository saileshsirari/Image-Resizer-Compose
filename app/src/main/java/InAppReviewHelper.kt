package com.image.resizer.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.StatsLog.logEvent
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.concurrent.TimeUnit
import androidx.core.content.edit
import com.image.resizer.compose.log
import com.image.resizer.utils.AnalyticsHelper.logReviewShown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object InAppReviewHelper {

    private const val TAG = "InAppReviewHelper"
    private const val PREFS_NAME = "InAppReviewPrefs"
    private const val LAST_REQUEST_TIME = "last_request_time"
    private const val REQUEST_COUNT = "request_count"

    private const val REVIEW_REQUEST_INTERVAL_DAYS = 3L
    private const val MIN_REVIEW_REQUEST_COUNT = 3


    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getRequestCount(context: Context): Int {
        val prefs = getSharedPreferences(context)
        return prefs.getInt(REQUEST_COUNT, MIN_REVIEW_REQUEST_COUNT)
    }

    private fun setRequestCount(context: Context, count: Int) {
        val prefs = getSharedPreferences(context)
        prefs.edit { putInt(REQUEST_COUNT, count) }
    }

    private fun getLastRequestTime(context: Context): Long {
        val prefs = getSharedPreferences(context)
        return prefs.getLong(LAST_REQUEST_TIME, 0L)
    }

    private fun setLastRequestTime(context: Context, time: Long) {
        val prefs = getSharedPreferences(context)
        prefs.edit { putLong(LAST_REQUEST_TIME, time) }
    }

    private fun shouldRequestReview(context: Context): Boolean {
        val requestCount = getRequestCount(context)
        if (requestCount > 0) {
            setRequestCount(context, requestCount - 1)
            return false
        }

        val lastRequestTime = getLastRequestTime(context)
        val currentTime = System.currentTimeMillis()

        val diffInDays = TimeUnit.MILLISECONDS.toDays(currentTime - lastRequestTime)
        return diffInDays >= REVIEW_REQUEST_INTERVAL_DAYS
    }

    suspend fun requestReview(context: Context): Boolean {

        if (shouldRequestReview(context)) {
            val reviewManager: ReviewManager = ReviewManagerFactory.create(context)
            try {
                val reviewInfo = reviewManager.requestReview()
                if (context is Activity) {
                    reviewManager.launchReview(context, reviewInfo)
                    coroutineScope {
                        logEvent(Dispatchers.IO) {
                            logReviewShown()
                        }
                    }

                    return true
                    log(tag = TAG, message = "Review Launched")
                } else {
                    log(tag = TAG, message = "Can't launch review because context is not Activity")
                }
                setLastRequestTime(context, System.currentTimeMillis())

            } catch (e: Exception) {
                log(
                    tag = TAG,
                    message = "Error requesting or launching in-app review: ${e.message}"
                )
            }
        } else {
            log(tag = TAG, message = "Can't request a review")

        }
        return false
    }
}
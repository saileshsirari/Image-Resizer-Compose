package com.image.resizer.utils

import android.app.Application
import com.image.resizer.utils.AnalyticsHelper

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AnalyticsHelper.initialize(this)
    }
}
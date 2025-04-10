package com.image.resizer.compose

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import java.text.DecimalFormat

fun getDeviceMemoryInfo(context: Context) {
    val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    actManager.getMemoryInfo(memInfo)

    val totalMemoryInMB = memInfo.totalMem / (1024.0 * 1024.0)
    val availableMemoryInMB = memInfo.availMem / (1024.0 * 1024.0)
    val thresholdInMB =
        memInfo.threshold / (1024.0 * 1024.0) // System will start killing processes if memory falls below this point
    val lowMemory = memInfo.lowMemory

    val decimalFormat = DecimalFormat("#.##")

    log(
        "Total Memory: ${decimalFormat.format(totalMemoryInMB)} MB\n" +
                "Available Memory: ${decimalFormat.format(availableMemoryInMB)} MB\n" +
                "Low Memory: $lowMemory\n" +
                "Threshold: ${decimalFormat.format(thresholdInMB)} MB"
    )
    val memClassMegs = actManager.memoryClass
    log("Memory class: $memClassMegs MB")

}

fun getHeapMemoryInfo() {
    val runtime = Runtime.getRuntime()
    val usedMemoryInMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0)
    val maxHeapSizeInMB = runtime.maxMemory() / (1024.0 * 1024.0)
    val freeMemoryInMB = runtime.freeMemory() / (1024.0 * 1024.0)
    val totalMemoryInMB = runtime.totalMemory() / (1024.0 * 1024.0)
    val decimalFormat = DecimalFormat("#.##")

    log(
        "MemoryInfo",
        "Used Memory: ${decimalFormat.format(usedMemoryInMB)} MB\n" +
                "Free Memory: ${decimalFormat.format(freeMemoryInMB)} MB\n" +
                "Max Heap Size: ${decimalFormat.format(maxHeapSizeInMB)} MB\n" +
                "Total Memory: ${decimalFormat.format(totalMemoryInMB)} MB"
    )
}

fun getDetailedMemoryInfo(context: Context) {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val processMemoryInfo = activityManager.getRunningAppProcesses().find {
        it.processName == context.packageName
    }?.pid?.let {
        activityManager.getProcessMemoryInfo(intArrayOf(it))[0]
    }
    processMemoryInfo?.let {
        val decimalFormat = DecimalFormat("#.##")
        val totalPssInMB = it.totalPss / 1024.0
        val totalPrivateDirtyInMB = it.totalPrivateDirty / 1024.0
        val totalSharedDirtyInMB = it.totalSharedDirty / 1024.0
        log(
            "Total Pss: ${decimalFormat.format(totalPssInMB)} MB\n" +
                    "Total Private Dirty: ${decimalFormat.format(totalPrivateDirtyInMB)} MB\n" +
                    "Total Shared Dirty: ${decimalFormat.format(totalSharedDirtyInMB)} MB\n"
        )

    } ?: run {
        Log.e("MemoryInfo", "Error getting process memory information")
    }
}
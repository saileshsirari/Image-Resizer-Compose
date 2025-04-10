package com.image.resizer.compose

import android.net.Uri
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

object UriMutexManager {
    private val mutexMap = ConcurrentHashMap<Uri, Mutex>()

    fun getMutex(uri: Uri): Mutex {
        return mutexMap.computeIfAbsent(uri) { Mutex() }
    }
}
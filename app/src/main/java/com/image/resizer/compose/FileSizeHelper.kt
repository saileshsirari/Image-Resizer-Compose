package com.image.resizer.compose

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileInputStream

object FileSizeHelper {
    fun getFileSizeFromUriUsingFileDescriptor(context: Context, uri: Uri): Long? {
        val contentResolver: ContentResolver = context.contentResolver
        var fileDescriptor: ParcelFileDescriptor? = null
        try {
            fileDescriptor = contentResolver.openFileDescriptor(uri, "r")

            if (fileDescriptor != null) {
                val fileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
                return fileInputStream.channel.size()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fileDescriptor?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }
}
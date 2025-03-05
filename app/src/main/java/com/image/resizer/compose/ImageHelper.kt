package com.image.resizer.compose

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.File

object ImageHelper {

    fun getFileNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                val name = it.getString(nameIndex)
                val size = it.getLong(sizeIndex)
                return Pair(name, size)
            }
        }
        return Pair("", 0L)
    }

    internal fun getRealCompressedImageUris(context: Context, indexesToLoad: List<Int>): List<ImageItem?> {
        val compressedImageUris = mutableListOf<ImageItem>()
        if (indexesToLoad.isEmpty()) return compressedImageUris

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
        )
        val  customDirectoryName: String="ImageResizer"
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("imageResizer_%")
//    val selectionArgs = arrayOf("%$customDirectoryName/%")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        cursor?.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val totalCount = cursor.count
            if (totalCount == 0) {
                return emptyList()
            }
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(queryUri, id)
                val imageItem = ImageItem(uri = contentUri, imageName = cursor.getString(nameColumn), fileSize = cursor.getLong(sizeColumn))
                compressedImageUris.add(imageItem)
            }
        }
        return compressedImageUris
    }

    fun getImagesFromCustomFolder(context: Context, folderName: String): List<Uri> {
        val imageUris = mutableListOf<Uri>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH // To get the folder path
        )
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$folderName/%")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val contentResolver = context.contentResolver
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.let {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val relativePathColumn =
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val relativePath = it.getString(relativePathColumn)
                    // Check if the image is in the correct folder.
                    if (relativePath.contains("$folderName${File.separator}")) {
                        val contentUri = ContentUris.withAppendedId(collection, id)
                        imageUris.add(contentUri)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ImageHelper", "Error reading images: ${e.message}", e)
        } finally {
            cursor?.close()
        }

        return imageUris
    }
}
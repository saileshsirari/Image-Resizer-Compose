package com.image.resizer.compose

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Log.e
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.SideEffect
import java.io.File
import java.io.FileOutputStream

object ImageReplacer {

    fun replaceOriginalImageWithBitmap(context: Context, originalUri: Uri, newBitmap: Bitmap): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return replaceImageQAndAbove(context, originalUri, newBitmap)
        } else {
            return replaceImageBelowQ(context, originalUri, newBitmap)
        }
    }
    @Composable
    @RequiresApi(Build.VERSION_CODES.R)
    fun deleteImage(context: Context, imageUri: Uri,onDeleted: () -> Unit): Boolean {
        return try {
            val contentResolver: ContentResolver = context.contentResolver

            // ContentResolver.delete() takes a content URI and an optional selection/selectionArgs
            // to target the specific row to delete.
            // the logic is the same but we need to change the uri to delete

            val modifiedUri = getModifiedUri(context,imageUri)

            //check if the uri has been properly modified
            if (modifiedUri == imageUri){
                Log.e("ImageDeleter", "Error deleting image: Fail to convert to MediaStore uri")
                return false
            }
            val rowsDeleted = contentResolver.delete(modifiedUri, null, null)

            if (rowsDeleted > 0) {
                Log.d("ImageDeleter", "Image deleted successfully: $modifiedUri")
                true
            } else {
                Log.e("ImageDeleter", "Failed to delete image: $modifiedUri")
                false
            }
        } catch (e: Exception) {
            Log.e("ImageDeleter", "Error deleting image: ${e.message}")
            e.printStackTrace()
            deleteSelectedImages(context, listOf(imageUri), onDeleted = { onDeleted() })
            false
        }
    }
    /**
     * This function modifies the uri from content://media/picker/0/com.android.providers.media.photopicker/media/1000125010 to content://media/external/images/media/1000125010
     */
    private fun getModifiedUri(context: Context, imageUri: Uri): Uri {
        val contentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = null
        val selectionArgs = null
        val cursor = contentResolver.query(imageUri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val imageId = it.getLong(idIndex)
                return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId.toString())
            }
        }
        return imageUri //return the original uri if the modify fail
    }

    @Composable
    fun deleteSelectedImages(
        context: Context,
        selectedImages: List<Uri>,
        onDeleted:()-> Unit,
    ) {
        var deletePendingUris by remember {
            mutableStateOf<List<Uri>>(
                emptyList()
            )
        }
        var deletePendingRecoverableSecurityException by remember {
            mutableStateOf<RecoverableSecurityException?>(
                null
            )
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Retry deletion for all uris
                if (deletePendingRecoverableSecurityException != null) {
                    deletePendingRecoverableSecurityException?.let { exception ->
                        deletePendingUris.forEach { uri ->
                            try {
                                context.contentResolver.delete(uri, null, null)
                            } catch (e: SecurityException) {
                                // Handle any further errors (e.g., log them)
                                e.printStackTrace()
                            }
                        }
                    }
                } else {
                    onDeleted()
                }

            } else {
                // Handle failure or user cancellation
                Log.e("MyImagesScreen", "Deletion failed or cancelled by user")
            }
            deletePendingUris = emptyList()
            deletePendingRecoverableSecurityException = null
        }
        val urisToDelete = mutableListOf<Uri>()
        val contentResolver = context.contentResolver

        val recoverableSecurityExceptions = mutableListOf<RecoverableSecurityException>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val trashIntent = MediaStore.createTrashRequest(
                contentResolver,
                selectedImages, true
            )
            val intentSenderRequest = IntentSenderRequest.Builder(trashIntent).build()
            SideEffect {
                launcher.launch(intentSenderRequest)
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            selectedImages.forEach { uri ->
                try {
                    contentResolver.delete(uri, null, null)
                } catch (securityException: SecurityException) {
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException
                    if (recoverableSecurityException != null) {
                        recoverableSecurityExceptions.add(recoverableSecurityException)
                        urisToDelete.add(uri)
                    }

                }
            }
            if (recoverableSecurityExceptions.isNotEmpty()) {
                val intentSender =
                    recoverableSecurityExceptions.first().userAction.actionIntent.intentSender
                val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                SideEffect {
                    launcher.launch(intentSenderRequest)
                }
            }

        } else {
            selectedImages.forEach { uri ->
                try {
                    contentResolver.delete(uri, null, null)
                } catch (securityException: SecurityException) {
                    securityException.printStackTrace()
                    // Handle the exception for pre-Q devices
                    // You can show a message to the user indicating that deletion failed.
                    // Example: Toast.makeText(context, "Deletion failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun replaceImageQAndAbove(context: Context, originalUri: Uri, newBitmap: Bitmap) : Boolean{
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            // Get the original file's details to maintain metadata (e.g., name, date)
            val projection = arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.RELATIVE_PATH
            )
            val selection = "${MediaStore.Images.Media._ID} = ?"
            val id = originalUri.lastPathSegment // extract id from the uri
            val selectionArgs = arrayOf(id)
            val cursor = contentResolver.query(collection, projection, selection, selectionArgs, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val mimeTypeIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                    val dateModifiedIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                    val relativePathIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)

                    val displayName = it.getString(displayNameIndex)
                    val mimeType = it.getString(mimeTypeIndex)
                    val dateModified = it.getLong(dateModifiedIndex)
                    val relativePath = it.getString(relativePathIndex)
                    // Update the ContentValues with the original file's metadata
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Images.Media.DATE_MODIFIED, dateModified)
                        put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                        put(MediaStore.Images.Media.IS_PENDING, 1) // To prevent immediate scanning
                    }
                    contentResolver.update(originalUri, values, null, null)

                    // Open an OutputStream to write to the file
                    contentResolver.openOutputStream(originalUri)?.use { outputStream ->
                        // Compress and save the new bitmap
                        if(mimeType.contains("png")){
                            newBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                        else{
                            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        }

                        outputStream.flush()
                    }
                    // Clear the IS_PENDING flag to make it scannable
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(originalUri, values, null, null)

                }
            }
        } catch (e: Exception) {
            Log.e("ImageReplacer", "Error replacing image (Q+): ${e.message}")
            e.printStackTrace()
            return false
        }
        return  true
    }

    private fun replaceImageBelowQ(context: Context, originalUri: Uri, newBitmap: Bitmap): Boolean {
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val projection = arrayOf(MediaStore.Images.Media.DATA)

            val cursor = contentResolver.query(originalUri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val dataColumnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val imagePath = it.getString(dataColumnIndex)
                    val file = File(imagePath)

                    // Check if the file exists and is writable
                    if (file.exists() && file.canWrite()) {
                        FileOutputStream(file).use { outputStream ->
                            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                            outputStream.flush()
                        }
                    }
                }
            }
            //Trigger media scanner
            context.sendBroadcast(
                Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(originalUri.path.orEmpty())))
            )
        } catch (e: Exception) {
            Log.e("ImageReplacer", "Error replacing image (Below Q): ${e.message}")
            e.printStackTrace()
            return  false
        }
        return  true
    }

    fun getBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("ImageHelper", "Error loading image from URI: ${e.message}")
            null
        }
    }
}
package com.image.resizer.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ExifHandler {
    private const val TAG = "ExifHandler"

    fun setExifDataAfterScaling(
        context: Context,
        originalImageUri: Uri,
        scaledBitmap: Bitmap,
        outputFile: File,
        orientation: Int = ExifInterface.ORIENTATION_NORMAL
    ) {
        try {
            val originalExif = getExif(context, originalImageUri)
            // Write the scaled image to the output file
            FileOutputStream(outputFile).use { outputStream ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }

            // Create ExifInterface for the output file
            val newExif = ExifInterface(outputFile.absolutePath)

            // Copy the original Exif attributes
            copyExifAttributes(originalExif, newExif)
            newExif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            // You can add or update more EXIF tags here
            // Example: Setting image width and height
            newExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, scaledBitmap.width.toString())
            newExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, scaledBitmap.height.toString())

            // Save the new EXIF data
            newExif.saveAttributes()
            Log.d(TAG, "Exif data saved successfully to: ${outputFile.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getExif(context: Context, selectedImage: Uri): ExifInterface {
        val input = context.contentResolver.openInputStream(selectedImage)
            ?: throw IOException("Could not open input stream")
        val ei = ExifInterface(input)
        input.close()
        return ei
    }

    private fun copyExifAttributes(fromExif: ExifInterface, toExif: ExifInterface) {
        val attributes = listOf(
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_FOCAL_LENGTH
        )

        attributes.forEach { attribute ->
            val value = fromExif.getAttribute(attribute)
            if (value != null) {
                toExif.setAttribute(attribute, value)
            }
        }
    }

}
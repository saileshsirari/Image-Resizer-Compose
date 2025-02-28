package com.image.resizer.compose

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions

class CropScreen : ComponentActivity() {

    companion object {
        const val CROPPED_IMAGE_BITMAP_URI = "cropped_image_bitmap"
        const val IMAGE_TO_CROP = "image_to_crop"
        const val IMAGE_TO_CROP_REQUEST_CODE = 1
        const val TAG = "CropScreen"
    }

    private var imageToCrop: Uri? = null
    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // use the returned uri
            val croppedUri = result.uriContent
            Log.d(TAG, "Cropped image URI: $croppedUri")
            val resultIntent = Intent()
            resultIntent.putExtra(CROPPED_IMAGE_BITMAP_URI, croppedUri)
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            // there was some error
            val error = result.error
            Log.d(TAG, "Error cropping image: $error")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the image URI to crop from the Intent
        imageToCrop = intent.getParcelableExtra(IMAGE_TO_CROP)
        if (imageToCrop != null) {
            cropImage.launch(
                CropImageContractOptions(imageToCrop!!, CropImageOptions())
            )
        } else {
            Log.e(TAG, "Image Uri is null")
            finish()
        }
    }
}
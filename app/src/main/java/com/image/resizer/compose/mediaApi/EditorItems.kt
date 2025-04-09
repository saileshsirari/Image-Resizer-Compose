package com.image.resizer.compose.mediaApi;

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.ReduceCapacity
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import apps.sai.com.imageresizer.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Parcelize
enum class EditorItems : Parcelable {
    Crop,
    Compress,
    Save,
    Replace,
    Back;

    @get:Composable
    val translatedName : String
        get() = when (this) {
            Crop -> stringResource(R.string.crop)
            Compress -> stringResource(R.string.compress)
            Back -> stringResource(R.string.back_cd)
            Save -> stringResource(R.string.save)
            Replace ->stringResource(R.string.replace)
        }

    @IgnoredOnParcel
    val icon: ImageVector
        get() = when (this) {
            Crop -> Icons.Outlined.Crop
            Compress -> Icons.Outlined.Compress
            Back -> Icons.AutoMirrored.Outlined.ArrowBack
            Replace -> Icons.Outlined.Recycling
            Save -> Icons.Outlined.Save
        }
}
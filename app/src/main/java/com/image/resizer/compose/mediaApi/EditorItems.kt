package com.image.resizer.compose.mediaApi;

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Filter
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
            Compress -> Icons.Outlined.Adjust
            Back -> Icons.Outlined.Draw
            Replace -> Icons.Outlined.Adjust
            Save -> Icons.Outlined.Draw
        }
}
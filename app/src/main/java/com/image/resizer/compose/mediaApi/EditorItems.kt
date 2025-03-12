package com.image.resizer.compose.mediaApi;

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.image.resizer.compose.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Parcelize
enum class EditorItems : Parcelable {
    Crop,
    Compress,
    Scale,
    Save,
    Replace,
    Undo;

    @get:Composable
    val translatedName : String
        get() = when (this) {
            Crop -> stringResource(R.string.crop)
            Compress -> stringResource(R.string.compress)
            Scale -> stringResource(R.string.scale)
            Undo -> stringResource(R.string.undo)
            Save -> stringResource(R.string.save)
            Replace ->stringResource(R.string.replace)
        }

    @IgnoredOnParcel
    val icon: ImageVector
        get() = when (this) {
            Crop -> Icons.Outlined.Crop
            Compress -> Icons.Outlined.Adjust
            Scale -> Icons.Outlined.Filter
            Undo -> Icons.Outlined.Draw
            Replace -> Icons.Outlined.Adjust
            Save -> Icons.Outlined.Draw
        }
}
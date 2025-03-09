package com.image.resizer.compose.mediaApi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.image.resizer.compose.R
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.util.Constants
import com.image.resizer.compose.mediaApi.util.ExifMetadata
import com.image.resizer.compose.mediaApi.util.getDate

@Stable
data class MediaDateCaption(
    val date: String,
    val deviceInfo: String? = null,
    val description: String
)

@Composable
fun rememberMediaDateCaption(
    exifMetadata: ExifMetadata?,
    media: Media
): MediaDateCaption {
    val deviceInfo = remember(exifMetadata) { exifMetadata?.lensDescription }
    val defaultDesc = stringResource(R.string.image_add_description)
    val description = remember(exifMetadata) { exifMetadata?.imageDescription ?: defaultDesc }
    val currentDateFormat by remember { mutableStateOf(Constants.EXIF_DATE_FORMAT) }
    return remember(media, currentDateFormat) {
        MediaDateCaption(
            date = media.definedTimestamp.getDate(currentDateFormat),
            deviceInfo = deviceInfo,
            description = description
        )
    }
}

@Composable
fun rememberMediaDateCaption(
    exifMetadata: ExifMetadata?,
    media: Media.UriMedia
): MediaDateCaption {
    val deviceInfo = remember(exifMetadata) { exifMetadata?.lensDescription }
    val defaultDesc = stringResource(R.string.image_add_description)
    val description = remember(exifMetadata) { exifMetadata?.imageDescription ?: defaultDesc }
    val currentDateFormat by remember { mutableStateOf(Constants.EXIF_DATE_FORMAT) }
    return remember(media, currentDateFormat) {
        MediaDateCaption(
            date = media.definedTimestamp.getDate(currentDateFormat),
            deviceInfo = deviceInfo,
            description = description
        )
    }
}
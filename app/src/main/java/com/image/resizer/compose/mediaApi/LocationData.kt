package com.image.resizer.compose.mediaApi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.image.resizer.compose.mediaApi.model.Media
import com.image.resizer.compose.mediaApi.model.formattedAddress
import com.image.resizer.compose.mediaApi.model.getLocation
import com.image.resizer.compose.mediaApi.model.rememberGeocoder
import com.image.resizer.compose.mediaApi.util.ExifMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Stable
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val location: String
)

@Composable
fun rememberLocationData(
    exifMetadata: ExifMetadata?,
    media: Media
): LocationData? {
    val geocoder = rememberGeocoder()
    var locationName by remember { mutableStateOf(exifMetadata?.formattedCords) }
    LaunchedEffect(geocoder, exifMetadata) {
        withContext(Dispatchers.IO) {
            if (exifMetadata?.gpsLatLong != null) {
                geocoder?.getLocation(
                    exifMetadata.gpsLatLong[0],
                    exifMetadata.gpsLatLong[1]
                ) { address ->
                    address?.let {
                        val addressName = it.formattedAddress
                        if (addressName.isNotEmpty()) {
                            locationName = addressName
                        }
                    }
                }
            }
        }
    }
    return remember(media, exifMetadata, locationName) {
        exifMetadata?.let {
            it.gpsLatLong?.let { latLong ->
                LocationData(
                    latitude = latLong[0],
                    longitude = latLong[1],
                    location = locationName ?: "Unknown"
                )
            }
        }
    }
}
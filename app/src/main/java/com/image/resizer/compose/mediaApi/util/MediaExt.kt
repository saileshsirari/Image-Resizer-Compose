package com.image.resizer.compose.mediaApi.util

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.image.resizer.compose.mediaApi.model.Media
import io.ktor.util.reflect.instanceOf

/**
 * Determine if the current media is a raw format
 *
 * Checks if [Media.mimeType] starts with "image/x-" or "image/vnd."
 *
 *
 *
 * Most used formats:
 * - ARW: image/x-sony-arw
 * - CR2: image/x-canon-cr2
 * - CRW: image/x-canon-crw
 * - DCR: image/x-kodak-dcr
 * - DNG: image/x-adobe-dng
 * - ERF: image/x-epson-erf
 * - K25: image/x-kodak-k25
 * - KDC: image/x-kodak-kdc
 * - MRW: image/x-minolta-mrw
 * - NEF: image/x-nikon-nef
 * - ORF: image/x-olympus-orf
 * - PEF: image/x-pentax-pef
 * - RAF: image/x-fuji-raf
 * - RAW: image/x-panasonic-raw
 * - SR2: image/x-sony-sr2
 * - SRF: image/x-sony-srf
 * - X3F: image/x-sigma-x3f
 *
 * Other proprietary image types in the standard:
 * image/vnd.manufacturer.filename_extension for instance for NEF by Nikon and .mrv for Minolta:
 * - NEF: image/vnd.nikon.nef
 * - Minolta: image/vnd.minolta.mrw
 */
val Media.isRaw: Boolean
    get() =
        mimeType.isNotBlank() && (mimeType.startsWith("image/x-") || mimeType.startsWith("image/vnd."))

private val Media.rawExtension: String
    get() = if (mimeType.startsWith("image/vnd."))
        mimeType.substringAfterLast(".").removePrefix(".") else mimeType.substringAfterLast("-")
        .removePrefix("-")

val Media.fileExtension: String
    get() = if (isRaw) rawExtension else label.substringAfterLast(".").removePrefix(".")

val Media.volume: String
    get() = path.substringBeforeLast("/").removeSuffix(relativePath.removeSuffix("/"))

@Composable
fun <T> rememberedDerivedState(
    key: Any? = Unit,
    block: @DisallowComposableCalls () -> T
): State<T> {
    return remember(key) {
        derivedStateOf(block)
    }
}
@Composable
fun <T> rememberedDerivedState(
    vararg keys: Any? = arrayOf(Unit),
    block: @DisallowComposableCalls () -> T
): State<T> {
    return remember(keys) {
        derivedStateOf(block)
    }
}
/**
 * Used to determine if the Media object is not accessible
 * via MediaStore.
 * This happens when the user tries to open media from an app
 * using external sources (in our case, Gallery Media Viewer), but
 * the specific media is only available internally in that app
 * (Android/data(OR media)/com.package.name/)
 *
 * If it's readUriOnly then we know that we should expect a barebone
 * Media object with limited functionality (no favorites, trash, timestamp etc)
 */

val Media.isVideo: Boolean get() = mimeType.startsWith("video/") && duration != null

val Media.isImage: Boolean get() = mimeType.startsWith("image/")

val Media.isTrashed: Boolean get() = trashed == 1

val Media.isFavorite: Boolean get() = favorite == 1

val Media.isEncrypted: Boolean
    get() = instanceOf(Media.UriMedia::class) /*&& getUri().toString()
        .contains(BuildConfig.APPLICATION_ID)*/

fun <T : Media> T.getUri(): Uri {
    return when (this) {
        is Media.UriMedia -> uri
        else -> throw IllegalArgumentException("Media type ${this.javaClass.simpleName} not supported")
    }
}


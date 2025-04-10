/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi.model

import android.net.Uri
import android.os.Parcelable
import com.image.resizer.compose.mediaApi.util.UriSerializer
import com.image.resizer.compose.mediaApi.util.getUri
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Serializable
@Parcelize
sealed class Media : Parcelable, java.io.Serializable {


    abstract val id: Long
    abstract val label: String
    abstract val path: String
    abstract val relativePath: String
    abstract val albumID: Long
    abstract val albumLabel: String
    abstract val timestamp: Long
    abstract val expiryTimestamp: Long?
    abstract val takenTimestamp: Long?
    abstract val fullDate: String
    abstract val mimeType: String
    abstract val favorite: Int
    abstract val trashed: Int
    abstract val size: Long
    abstract val duration: String?

    val definedTimestamp: Long
        get() = takenTimestamp?.div(1000) ?: timestamp

    override fun toString(): String {
        return "$id, $path, $fullDate, $mimeType, $definedTimestamp"
    }

    val key: String
        get() = "{$id, ${try { getUri() } catch (_: Exception) { path} }, $definedTimestamp}"

    val idLessKey: String
        get() = "{${try { getUri() } catch (_: Exception) { path} }, $definedTimestamp}"

    @Serializable
    @Parcelize
    data class UriMedia(
        override val id: Long = 0,
        override val label: String,
        @Serializable(with = UriSerializer::class)
        val uri: Uri,
        override val path: String,
        override val relativePath: String,
        override val albumID: Long,
        override val albumLabel: String,
        override val timestamp: Long,
        override val expiryTimestamp: Long? = null,
        override val takenTimestamp: Long? = null,
        override val fullDate: String,
        override val mimeType: String,
        override val favorite: Int,
        override val trashed: Int,
        override val size: Long,
        override val duration: String? = null
    ) : Media()

}

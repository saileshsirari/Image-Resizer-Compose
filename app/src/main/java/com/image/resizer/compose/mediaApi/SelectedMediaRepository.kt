package com.image.resizer.compose.mediaApi

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "selected_media_prefs")

@Serializable
data class SelectedMediaUris(
    val uris: List<String>
) {
    companion object {
        fun fromUriList(uris: List<Uri>): SelectedMediaUris {
            return SelectedMediaUris(uris.map { it.toString() })
        }
    }

    fun toUriList(): List<Uri> = uris.map { Uri.parse(it) }
}

class SelectedMediaRepository(private val context: Context) {
    companion object {
        val SELECTED_MEDIA_URIS_KEY = stringPreferencesKey("selected_media_uris")
    }

    suspend fun addSelectedMedia(uri: Uri) {
        context.dataStore.edit { preferences ->
            val currentSelectedMediaUris = preferences[SELECTED_MEDIA_URIS_KEY]?.let {
                Json.decodeFromString<SelectedMediaUris>(it)
            } ?: SelectedMediaUris(emptyList<String>())

            val updatedSelectedMediaUris =
                SelectedMediaUris(currentSelectedMediaUris.uris + uri.toString())
            preferences[SELECTED_MEDIA_URIS_KEY] = Json.encodeToString(updatedSelectedMediaUris)
        }
    }

    suspend fun addSelectedMedias(uris: List<Uri>) {
        context.dataStore.edit { preferences ->
            val updatedSelectedMediaUris =
                SelectedMediaUris(uris.map { it.toString() })
            preferences[SELECTED_MEDIA_URIS_KEY] = Json.encodeToString(updatedSelectedMediaUris)
        }
    }

    suspend fun removeSelectedMedia(uri: Uri) {
        context.dataStore.edit { preferences ->
            val currentSelectedMediaUris = preferences[SELECTED_MEDIA_URIS_KEY]?.let {
                Json.decodeFromString<SelectedMediaUris>(it)
            } ?: SelectedMediaUris(emptyList<String>())
            val updatedSelectedMediaUris =
                SelectedMediaUris(currentSelectedMediaUris.uris.filter { it != uri.toString() })
            preferences[SELECTED_MEDIA_URIS_KEY] = Json.encodeToString(updatedSelectedMediaUris)
        }
    }

    suspend fun clearSelectedMedia() {
        context.dataStore.edit { preferences ->
            val currentSelectedMediaUris = SelectedMediaUris(emptyList<String>())
            preferences[SELECTED_MEDIA_URIS_KEY] = Json.encodeToString(currentSelectedMediaUris)
        }
    }

    fun getSelectedMedia(): Flow<List<Uri>> {
        return context.dataStore.data.map { preferences ->
            val selectedMediaJson = preferences[SELECTED_MEDIA_URIS_KEY]
            if (selectedMediaJson != null) {
                val selectedMediaUris = Json.decodeFromString<SelectedMediaUris>(selectedMediaJson)
                selectedMediaUris.toUriList()
            } else {
                emptyList()
            }
        }
    }
}
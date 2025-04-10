package com.image.resizer.compose.mediaApi.model

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.image.resizer.compose.mediaApi.util.Constants.albumCellsList
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

object Settings {

    const val PREFERENCE_NAME = "settings"

    object Album {



    }
}
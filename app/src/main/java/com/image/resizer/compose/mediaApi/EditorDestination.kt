package com.image.resizer.compose.mediaApi

import kotlinx.serialization.Serializable

@Serializable
sealed class EditorDestination {

    @Serializable
    data object Editor : EditorDestination()

    @Serializable
    data object Crop : EditorDestination()

    @Serializable
    data object Scale : EditorDestination()


    @Serializable
    data object Compress : EditorDestination()

    @Serializable
    data object Undo : EditorDestination()

        @Serializable
        data object ExternalEditor : EditorDestination()

}
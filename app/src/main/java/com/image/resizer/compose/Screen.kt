package com.image.resizer.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String?="", val icon: ImageVector?=null) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object MyImages : Screen("my_images", "My Images", Icons.Filled.Image)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    data object TimelineScreen : Screen("timeline_screen")
    data object SettingsScreen : Screen("settings_screen")
    data object LibraryScreen : Screen("library_screen")
    data object AlbumsScreen : Screen("albums_screen")
    data object CategoriesScreen : Screen("categories_screen")

    data object CategoryViewScreen : Screen("category_view_screen") {

        fun category() = "$route?category={category}"

        fun category(string: String) = "$route?category=$string"

    }
    data object AlbumViewScreen : Screen("album_view_screen") {

        fun albumAndName() = "$route?albumId={albumId}&albumName={albumName}"

    }
    data object MediaViewScreen : Screen("media_screen") {

        fun idAndTarget() = "$route?mediaId={mediaId}&target={target}"

        fun idAndAlbum() = "$route?mediaId={mediaId}&albumId={albumId}"

        fun idAndQuery() = "$route?mediaId={mediaId}&query={query}"

        fun idAndCategory() = "$route?mediaId={mediaId}&category={category}"

        fun idAndCategory(id: Long, category: String) = "$route?mediaId=$id&category=$category"
    }
}
package com.image.resizer.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object MyImages : Screen("my_images", "My Images", Icons.Filled.Image)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}
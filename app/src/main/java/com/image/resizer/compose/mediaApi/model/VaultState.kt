package com.image.resizer.compose.mediaApi.model

import androidx.compose.runtime.Stable

@Stable
data class VaultState(
    val vaults: List<Vault> = emptyList(),
    val isLoading: Boolean = true
) {

    fun getStartScreen(): String {
        return (if (isLoading) VaultScreens.LoadingScreen else if (vaults.isEmpty()) {
            VaultScreens.VaultSetup
        } else {
            VaultScreens.VaultDisplay
        }).invoke()
    }
}
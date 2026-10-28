package com.kidloop.app.data.model

data class KidLoopSettings(
    val pinSalt: String = "",
    val pinHash: String = "",
    val sessionLimitMinutes: Int = 20,
    val dailyLimitMinutes: Int = 60,
    val allowVolumeKeys: Boolean = true,
    val isKidModeActive: Boolean = false,
    val items: List<PlaylistItem> = emptyList()
) {
    val isPinConfigured: Boolean get() = pinHash.isNotBlank() && pinSalt.isNotBlank()
    val canStartKidMode: Boolean get() = isPinConfigured && items.isNotEmpty()
}

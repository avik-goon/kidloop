package com.kidloop.app.data.model

enum class ItemType {
    VIDEO,
    PLAYLIST
}

data class PlaylistItem(
    val id: String,
    val type: ItemType,
    val title: String,
    val originalUrl: String,
    val isEnabled: Boolean = true
) {
    /**
     * Serializes this item to a delimited string format:
     * id|type|title|originalUrl|isEnabled
     */
    fun toSerializedString(): String {
        val safeTitle = title.replace("|", "/")
        val safeUrl = originalUrl.replace("|", "/")
        return "$id|${type.name}|$safeTitle|$safeUrl|$isEnabled"
    }

    companion object {
        fun fromSerializedString(str: String): PlaylistItem? {
            val parts = str.split("|")
            if (parts.size < 5) return null
            val id = parts[0]
            val type = try {
                ItemType.valueOf(parts[1])
            } catch (_: Exception) {
                ItemType.VIDEO
            }
            val title = parts[2]
            val originalUrl = parts[3]
            val isEnabled = parts[4].toBooleanStrictOrNull() ?: true
            return PlaylistItem(
                id = id,
                type = type,
                title = title,
                originalUrl = originalUrl,
                isEnabled = isEnabled
            )
        }
    }
}

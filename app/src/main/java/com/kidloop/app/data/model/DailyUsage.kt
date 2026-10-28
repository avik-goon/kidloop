package com.kidloop.app.data.model

data class DailyUsage(
    val dateString: String = "",
    val usedSecondsToday: Long = 0L
) {
    val usedMinutesToday: Int get() = (usedSecondsToday / 60).toInt()
}

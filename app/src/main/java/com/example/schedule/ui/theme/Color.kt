package com.example.schedule.ui.theme

import androidx.compose.ui.graphics.Color

// 莫兰迪课程配色（所有主题共用）
private val BaseColors = listOf(
    Color(0xFFB5C7D3), Color(0xFFC4D4C0), Color(0xFFDDD0BC), Color(0xFFD4C2D4),
    Color(0xFFE8C5B8), Color(0xFFC5D0D9), Color(0xFFD9CFC0), Color(0xFFC8CCD0),
    Color(0xFFC9D7B3), Color(0xFFE0CDC5), Color(0xFFC2D4CB), Color(0xFFDED5E3),
)

fun getAvailableColors(usedColorValues: Set<Int>): List<Color> {
    val available = BaseColors.filter { it.hashCode() !in usedColorValues }.toMutableList()
    var h = 0
    while (available.size < 6) {
        val hue = (h * 37) % 360
        val new = Color.hsl(hue.toFloat(), 0.25f, 0.75f)
        if (new.hashCode() !in usedColorValues) available.add(new)
        h++
    }
    return available
}

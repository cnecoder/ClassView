package com.example.schedule.ui.theme

import androidx.compose.ui.graphics.Color

// 莫兰迪主色调
val MorandiPrimary = Color(0xFF7B8FA1)       // 灰蓝
val MorandiOnPrimary = Color(0xFFFFFFFF)
val MorandiSecondary = Color(0xFFA8B5A2)     // 灰绿
val MorandiTertiary = Color(0xFFC4A882)      // 灰棕
val MorandiBackground = Color(0xFFF5F0EB)    // 暖灰底色
val MorandiSurface = Color(0xFFFDFBF8)       // 奶油白
val MorandiSurfaceVariant = Color(0xFFF0EBE3) // 略深底色
val MorandiOnSurface = Color(0xFF3D3929)     // 深灰文字
val MorandiOnSurfaceVariant = Color(0xFF8B8578) // 浅灰文字
val MorandiError = Color(0xFFC2857C)         // 灰粉红

// 莫兰迪课程配色
private val BaseColors = listOf(
    Color(0xFFB5C7D3), Color(0xFFC4D4C0), Color(0xFFDDD0BC), Color(0xFFD4C2D4),
    Color(0xFFE8C5B8), Color(0xFFC5D0D9), Color(0xFFD9CFC0), Color(0xFFC8CCD0),
    Color(0xFFC9D7B3), Color(0xFFE0CDC5), Color(0xFFC2D4CB), Color(0xFFDED5E3),
)

/** 获取可用颜色列表，排除已使用的颜色，不够时自动生成 */
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

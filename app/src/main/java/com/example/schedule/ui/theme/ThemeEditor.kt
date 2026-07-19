package com.example.schedule.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.UUID

private val ColorPalette = listOf(
    0xFFE57373.toInt(), 0xFFF06292.toInt(), 0xFFE04090.toInt(), 0xFFF48FB1.toInt(), 0xFFFF8A80.toInt(),
    0xFFFFB74D.toInt(), 0xFFFF9800.toInt(), 0xFFFFC107.toInt(), 0xFFFFD54F.toInt(), 0xFFFF7043.toInt(),
    0xFF81C784.toInt(), 0xFF4CAF50.toInt(), 0xFF66BB6A.toInt(), 0xFF009688.toInt(), 0xFF26A69A.toInt(),
    0xFF4DD0E1.toInt(), 0xFF29B6F6.toInt(), 0xFF42A5F5.toInt(), 0xFF5C6BC0.toInt(), 0xFF3F51B5.toInt(),
    0xFF7E57C2.toInt(), 0xFFAB47BC.toInt(), 0xFFCE93D8.toInt(), 0xFFBA68C8.toInt(), 0xFF8E24AA.toInt(),
    0xFF90A4AE.toInt(), 0xFF78909C.toInt(), 0xFF8D6E63.toInt(), 0xFFA1887F.toInt(), 0xFFBCAAA4.toInt(),
    0xFFFFFFFF.toInt(), 0xFF212121.toInt(), 0xFF424242.toInt(), 0xFFF5F5F5.toInt(), 0xFFEEEEEE.toInt(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeEditorDialog(
    editKey: String? = null,
    editName: String? = null,
    editColors: Map<String, Color>? = null,
    onDismiss: () -> Unit,
    onSaved: (key: String) -> Unit,
) {
    val context = LocalContext.current
    val initColors = editColors ?: ThemeManager.savedThemeToColors(defaultSavedTheme())
    var name by remember { mutableStateOf(editName ?: "") }
    var primary by remember { mutableStateOf(initColors["primary"] ?: Color(0xFF7B8FA1.toInt())) }
    var secondary by remember { mutableStateOf(initColors["secondary"] ?: Color(0xFFA8B5A2.toInt())) }
    var tertiary by remember { mutableStateOf(initColors["tertiary"] ?: Color(0xFFC4A882.toInt())) }
    var background by remember { mutableStateOf(initColors["background"] ?: Color(0xFFF5F0EB.toInt())) }
    var surface by remember { mutableStateOf(initColors["surface"] ?: Color(0xFFFDFBF8.toInt())) }
    var surfaceVariant by remember { mutableStateOf(initColors["surfaceVariant"] ?: Color(0xFFF0EBE3.toInt())) }
    var onSurface by remember { mutableStateOf(initColors["onSurface"] ?: Color(0xFF3D3929.toInt())) }
    var onSurfaceVariant by remember { mutableStateOf(initColors["onSurfaceVariant"] ?: Color(0xFF8B8578.toInt())) }
    var outline by remember { mutableStateOf(initColors["outline"] ?: Color(0xFFD5CFC4.toInt())) }
    var activeSlot by remember { mutableStateOf<String?>(null) }
    var customHex by remember { mutableStateOf("") }

    val allColors = buildMap {
        put("primary", primary); put("secondary", secondary); put("tertiary", tertiary)
        put("background", background); put("surface", surface); put("surfaceVariant", surfaceVariant)
        put("onSurface", onSurface); put("onSurfaceVariant", onSurfaceVariant); put("outline", outline)
    }

    val slots = listOf(
        "primary" to "主色", "secondary" to "辅色", "tertiary" to "点缀色",
        "background" to "背景色", "surface" to "卡片色", "surfaceVariant" to "浅底色",
        "onSurface" to "文字色", "onSurfaceVariant" to "次要文字", "outline" to "边框色",
    )

    fun pickColor(key: String): (Color) -> Unit = { c ->
        when (key) {
            "primary" -> primary = c
            "secondary" -> secondary = c
            "tertiary" -> tertiary = c
            "background" -> background = c
            "surface" -> surface = c
            "surfaceVariant" -> surfaceVariant = c
            "onSurface" -> onSurface = c
            "onSurfaceVariant" -> onSurfaceVariant = c
            "outline" -> outline = c
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editKey != null) "编辑主题" else "新建主题", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = {
                    TextButton(onClick = {
                        if (name.isBlank()) return@TextButton
                        val key = editKey ?: "custom_${UUID.randomUUID().toString().take(8)}"
                        val saved = ThemeManager.colorsToSavedTheme(key, name, allColors)
                        ThemeManager.saveCustomTheme(context, key, name, saved)
                        ThemeManager.setCurrentTheme(context, key)
                        onSaved(key)
                        onDismiss()
                    }) { Text("保存", fontWeight = FontWeight.SemiBold) }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("主题名称") }, placeholder = { Text("如：我的配色") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)
            )

            // 预览（高亮正在编辑的元素）
            ThemePreview(allColors, activeKey = activeSlot)

            // 色槽列表 + 内联色板
            slots.forEach { (key, label) ->
                val isActive = activeSlot == key
                val color = allColors[key] ?: Color.Gray

                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
                        .border(if (isActive) 1.5.dp else 0.dp,
                            if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(12.dp))
                ) {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            activeSlot = if (isActive) null else key
                        }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(Modifier.size(32.dp), shape = CircleShape, color = color,
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.Gray.copy(alpha = 0.3f))) {}
                        Spacer(Modifier.width(12.dp))
                        Text(label, Modifier.weight(1f))
                        Text("#${Integer.toHexString(color.toArgb()).takeLast(6).uppercase()}",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // 展开的色板 + 自定义输入
                    if (isActive) {
                        Divider()
                        // 自定义 hex 输入
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customHex, onValueChange = { customHex = it },
                                label = { Text("自定义 #") }, placeholder = { Text("RRGGBB") },
                                modifier = Modifier.weight(1f), singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = {
                                try {
                                    val v = Integer.parseInt(customHex.trimStart('#'), 16)
                                    pickColor(key)(Color(0xFF000000.toInt() or v))
                                } catch (_: Exception) {}
                            }) { Text("应用", style = MaterialTheme.typography.labelMedium) }
                        }
                        // 预设色板
                        val cols = 5
                        val rows = (ColorPalette.size + cols - 1) / cols
                        Column(
                            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (r in 0 until rows) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (c in 0 until cols) {
                                        val idx = r * cols + c
                                        if (idx < ColorPalette.size) {
                                            val clr = Color(ColorPalette[idx])
                                            Surface(Modifier.size(36.dp).clickable {
                                                pickColor(key)(clr)
                                            }, shape = CircleShape, color = clr,
                                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Gray.copy(alpha = 0.3f))) {}
                                        } else { Spacer(Modifier.size(36.dp)) }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun defaultSavedTheme() = ThemeManager.SavedTheme(
    key = "", name = "",
    primary = 0xFF7B8FA1.toInt(), secondary = 0xFFA8B5A2.toInt(), tertiary = 0xFFC4A882.toInt(),
    background = 0xFFF5F0EB.toInt(), surface = 0xFFFDFBF8.toInt(), surfaceVariant = 0xFFF0EBE3.toInt(),
    onSurface = 0xFF3D3929.toInt(), onSurfaceVariant = 0xFF8B8578.toInt(), outline = 0xFFD5CFC4.toInt(),
)

@Composable
private fun ThemePreview(colors: Map<String, Color>, activeKey: String?) {
    val p = colors["primary"] ?: Color.Gray
    val s = colors["secondary"] ?: Color.Gray
    val t = colors["tertiary"] ?: Color.Gray
    val bg = colors["background"] ?: Color.White
    val sfv = colors["surfaceVariant"] ?: Color.LightGray
    val os = colors["onSurface"] ?: Color.Black
    val osv = colors["onSurfaceVariant"] ?: Color.Gray

    val infiniteAnim = rememberInfiniteTransition(label = "flash")
    val flashAlpha by infiniteAnim.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "flashAlpha"
    )
    fun Modifier.highlightIf(vararg keys: String): Modifier = this.then(
        if (activeKey != null && activeKey in keys)
            Modifier.border(2.dp, Color(0xFFFF6D00).copy(alpha = flashAlpha), RoundedCornerShape(6.dp))
        else Modifier
    )

    Card(Modifier.fillMaxWidth().highlightIf("background").padding(2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg)) {
        Column {
            // 顶部栏 — primary 控制 FAB 颜色
            Surface(Modifier.fillMaxWidth().height(40.dp), color = bg) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("课程表",
                        color = os, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.highlightIf("onSurface"))
                    Spacer(Modifier.weight(1f))
                    Surface(
                        Modifier.size(28.dp).highlightIf("primary"),
                        shape = CircleShape, color = p
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("+", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            // Tab — surfaceVariant 底色
            Surface(Modifier.fillMaxWidth().height(28.dp).highlightIf("surfaceVariant"), color = sfv) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("本周", color = p, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("月视图", color = osv, style = MaterialTheme.typography.labelMedium)
                    Text("列表", color = osv, style = MaterialTheme.typography.labelMedium)
                }
            }
            // 星期 — onSurfaceVariant 文字
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                listOf("一","二","三","四","五").forEach { d ->
                    Text(d, Modifier.weight(1f).highlightIf("onSurfaceVariant"),
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = osv)
                }
            }
            // 课程卡片
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(2.dp)
                    .highlightIf("surface", "primary"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = p.copy(alpha = 0.18f))) {
                    Row(Modifier.fillMaxWidth().padding(10.dp).highlightIf("surface"),
                        verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(8.dp), shape = CircleShape, color = p) {}
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("课程名称", color = os, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text("08:00 - 09:30", color = osv, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                // secondary 色 demo
                Surface(Modifier.size(24.dp).highlightIf("secondary"), shape = CircleShape, color = s) {}
            }
            // 底部按钮 — tertiary 也展示
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(Modifier.weight(1f).height(24.dp).highlightIf("surfaceVariant"),
                    shape = RoundedCornerShape(8.dp), color = sfv) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("按钮", color = osv, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Surface(Modifier.size(24.dp).highlightIf("tertiary"), shape = CircleShape, color = t) {}
            }
            // 边框示意
            Box(Modifier.fillMaxWidth().height(1.dp).highlightIf("outline").background(osv.copy(alpha = 0.15f)))
            Spacer(Modifier.height(4.dp))
        }
    }
}

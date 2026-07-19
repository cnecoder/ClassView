package com.example.schedule.ui.theme

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

data class ColorSlot(
    val key: String,
    val label: String,
    val currentColor: Color,
    val onPick: (Color) -> Unit
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
    val initColors = editColors ?: ThemeManager.savedThemeToColors(
        ThemeManager.loadAllCustomThemes(context).firstOrNull()
        ?: ThemeManager.SavedTheme(key = "", name = "", primary = 0xFF7B8FA1.toInt(), secondary = 0xFFA8B5A2.toInt(), tertiary = 0xFFC4A882.toInt(), background = 0xFFF5F0EB.toInt(), surface = 0xFFFDFBF8.toInt(), surfaceVariant = 0xFFF0EBE3.toInt(), onSurface = 0xFF3D3929.toInt(), onSurfaceVariant = 0xFF8B8578.toInt(), outline = 0xFFD5CFC4.toInt())
    )
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
    var editingSlot by remember { mutableStateOf<Pair<String, (Color) -> Unit>?>(null) }

    val slots = listOf(
        ColorSlot("primary", "主色", primary) { primary = it },
        ColorSlot("secondary", "辅色", secondary) { secondary = it },
        ColorSlot("tertiary", "点缀色", tertiary) { tertiary = it },
        ColorSlot("background", "背景色", background) { background = it },
        ColorSlot("surface", "卡片色", surface) { surface = it },
        ColorSlot("surfaceVariant", "浅底色", surfaceVariant) { surfaceVariant = it },
        ColorSlot("onSurface", "文字色", onSurface) { onSurface = it },
        ColorSlot("onSurfaceVariant", "次要文字", onSurfaceVariant) { onSurfaceVariant = it },
        ColorSlot("outline", "边框色", outline) { outline = it },
    )

    val allColors = mapOf(
        "primary" to primary, "secondary" to secondary, "tertiary" to tertiary,
        "background" to background, "surface" to surface, "surfaceVariant" to surfaceVariant,
        "onSurface" to onSurface, "onSurfaceVariant" to onSurfaceVariant, "outline" to outline
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editKey != null) "编辑主题" else "新建主题", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, "返回") }
                },
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
            Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 名称输入
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("主题名称") },
                placeholder = { Text("如：我的莫兰迪") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // 预览
            ThemePreview(allColors)

            Spacer(Modifier.height(4.dp))

            // 色槽
            slots.forEach { slot ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { editingSlot = slot.key to slot.onPick }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(Modifier.size(32.dp), shape = CircleShape, color = slot.currentColor,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Gray.copy(alpha = 0.3f))) {}
                    Spacer(Modifier.width(12.dp))
                    Text(slot.label, Modifier.weight(1f))
                    Text("#${Integer.toHexString(slot.currentColor.toArgb()).takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // 色板弹窗
    if (editingSlot != null) {
        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = { Text("选择颜色") },
            text = {
                val cols = 5
                val rows = (ColorPalette.size + cols - 1) / cols
                Column(Modifier.height(320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (r in 0 until rows) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (c in 0 until cols) {
                                val idx = r * cols + c
                                if (idx < ColorPalette.size) {
                                    val clr = Color(ColorPalette[idx])
                                    Surface(Modifier.size(48.dp).clickable {
                                        editingSlot?.second?.invoke(clr); editingSlot = null
                                    }, shape = CircleShape, color = clr,
                                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Gray.copy(alpha = 0.3f))) {}
                                } else { Spacer(Modifier.size(48.dp)) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editingSlot = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun ThemePreview(colors: Map<String, Color>) {
    val p = colors["primary"] ?: Color.Gray
    val s = colors["secondary"] ?: Color.Gray
    val t = colors["tertiary"] ?: Color.Gray
    val bg = colors["background"] ?: Color.White
    val sfv = colors["surfaceVariant"] ?: Color.LightGray
    val os = colors["onSurface"] ?: Color.Black
    val osv = colors["onSurfaceVariant"] ?: Color.Gray

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg)) {
        Column {
            Surface(Modifier.fillMaxWidth().height(40.dp), color = bg) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("课程表", color = os, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Surface(Modifier.size(28.dp), shape = CircleShape, color = p) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("+", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            Surface(Modifier.fillMaxWidth().height(28.dp), color = sfv) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("本周", color = p, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("月视图", color = osv, style = MaterialTheme.typography.labelMedium)
                    Text("列表", color = osv, style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                listOf("一","二","三","四","五").forEach { d ->
                    Text(d, Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, color = osv)
                }
            }
            Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = p.copy(alpha = 0.18f))) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(8.dp), shape = CircleShape, color = p) {}
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("课程名称", color = os, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("08:00 - 09:30", color = osv, style = MaterialTheme.typography.bodySmall)
                    }
                    Surface(Modifier.size(18.dp), shape = CircleShape, color = t) {}
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("编辑", "删除").forEach { label ->
                    Surface(Modifier.weight(1f).height(24.dp), shape = RoundedCornerShape(8.dp), color = sfv) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(label, color = osv, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

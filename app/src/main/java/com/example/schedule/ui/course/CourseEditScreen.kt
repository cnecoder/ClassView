package com.example.schedule.ui.course

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.schedule.data.model.Course
import com.example.schedule.ui.theme.getAvailableColors
import com.example.schedule.util.DateUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditScreen(
    course: Course,
    usedColors: Set<Int>,
    onSave: (Course) -> Unit,
    onCancel: () -> Unit
) {
    val availColors = remember(usedColors) { getAvailableColors(usedColors) }
    var name by remember { mutableStateOf(course.name) }
    var selectedDays by remember { mutableStateOf(DateUtils.parseDaySet(course.daysOfWeek)) }
    var startTime by remember { mutableStateOf(course.startTime) }
    var durationMin by remember {
        val s = DateUtils.timeToMinutes(course.startTime)
        val e = DateUtils.timeToMinutes(course.endTime)
        mutableIntStateOf((e - s).coerceIn(15, 180))
    }
    var startDate by remember { mutableStateOf(course.startDate) }
    var endDate by remember { mutableStateOf(course.endDate) }
    var skipHolidays by remember { mutableStateOf(course.skipHolidays) }
    var enableAlarm by remember { mutableStateOf(course.enableAlarm) }
    var alarmMinutesBefore by remember { mutableIntStateOf(course.alarmMinutesBefore) }
    var alarmRepeatInterval by remember { mutableIntStateOf(course.alarmRepeatInterval) }
    var alarmRepeatCount by remember { mutableIntStateOf(course.alarmRepeatCount) }
    var selectedColorIdx by remember(availColors) {
        val idx = availColors.indexOfFirst { it.hashCode() == course.color }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }
    var note by remember { mutableStateOf(course.note) }
    val selectedColorVal = availColors.getOrElse(selectedColorIdx) { availColors[0] }.hashCode()

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val isNew = course.id == 0L
    val dateFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun daysToStr(days: Set<Int>) = days.sorted().joinToString(",")
    fun toggleDay(days: Set<Int>, day: Int) =
        if (day in days) days - day else days + day

    fun parseTime(s: String): Pair<Int, Int> {
        val p = s.split(":")
        return (p.getOrNull(0)?.toIntOrNull() ?: 8) to (p.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    fun dateToMs(s: String) = try {
        LocalDate.parse(s, dateFmt).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: Exception) { System.currentTimeMillis() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "添加课程" else "编辑课程", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, "返回") }
                },
                actions = {
                    TextButton(onClick = {
                        val computedEndTime = DateUtils.minutesToTime(
                            DateUtils.timeToMinutes(startTime) + durationMin
                        )
                        onSave(course.copy(
                            name = name, daysOfWeek = daysToStr(selectedDays),
                            startTime = startTime, endTime = computedEndTime,
                            startDate = startDate, endDate = endDate,
                            skipHolidays = skipHolidays,
                            enableAlarm = enableAlarm,
                            alarmMinutesBefore = alarmMinutesBefore,
                            alarmRepeatInterval = alarmRepeatInterval,
                            alarmRepeatCount = alarmRepeatCount,
                            color = selectedColorVal, note = note
                        ))
                    }) {
                        Text("保存", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ===== 基本信息 =====
            SectionCard(title = "基本信息") {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("课程名称") }, placeholder = { Text("如：高等数学") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 时间
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PickerTrigger(label = "开始时间", value = startTime,
                        modifier = Modifier.weight(1f),
                        onClick = { showStartTimePicker = true })
                    PickerTrigger(label = "课程时长", value = "${durationMin}分钟",
                        modifier = Modifier.weight(1f),
                        onClick = { showDurationPicker = true })
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 日期
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PickerTrigger(label = "开始日期", value = startDate,
                        modifier = Modifier.weight(1f),
                        onClick = { showStartDatePicker = true })
                    PickerTrigger(label = "结束日期", value = endDate,
                        modifier = Modifier.weight(1f),
                        onClick = { showEndDatePicker = true })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 上课星期
                DayChips(weekDays, selectedDays) { selectedDays = toggleDay(selectedDays, it) }

                Spacer(modifier = Modifier.height(14.dp))

                // 颜色
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    availColors.forEachIndexed { index, color ->
                        Surface(
                            modifier = Modifier.size(32.dp).clickable { selectedColorIdx = index },
                            shape = CircleShape, color = color,
                            border = if (index == selectedColorIdx)
                                androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                            else androidx.compose.foundation.BorderStroke(1.dp, Color.Transparent)
                        ) {
                            if (index == selectedColorIdx)
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 跳过节假日
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("跳过法定节假日", Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = skipHolidays, onCheckedChange = { skipHolidays = it })
                }
            }

            // ===== 时间滚轮弹窗 =====
            if (showStartTimePicker) {
                val (h, m) = parseTime(startTime)
                ScrollWheelTimeDialog(
                    title = "开始时间",
                    initialHour = h, initialMinute = m,
                    onDismiss = { showStartTimePicker = false },
                    onConfirm = { hh, mm ->
                        startTime = "%02d:%02d".format(hh, mm)
                        showStartTimePicker = false
                    }
                )
            }
            if (showDurationPicker) {
                ScrollWheelDurationDialog(
                    title = "课程时长",
                    initialValue = durationMin,
                    onDismiss = { showDurationPicker = false },
                    onConfirm = { dur ->
                        durationMin = dur
                        showDurationPicker = false
                    }
                )
            }

            // ===== 日期选择器弹窗 =====
            if (showStartDatePicker) {
                val ms = dateToMs(startDate)
                val state = rememberDatePickerState(
                    initialSelectedDateMillis = ms,
                    selectableDates = object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
                    }
                )
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            state.selectedDateMillis?.let {
                                startDate = java.time.Instant.ofEpochMilli(it)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate().format(dateFmt)
                            }
                            showStartDatePicker = false
                        }) { Text("确定") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartDatePicker = false }) { Text("取消") }
                    }
                ) { DatePicker(state = state, title = { Text("开始日期") }) }
            }
            if (showEndDatePicker) {
                val ms = dateToMs(endDate)
                val state = rememberDatePickerState(
                    initialSelectedDateMillis = ms,
                    selectableDates = object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
                    }
                )
                DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            state.selectedDateMillis?.let {
                                endDate = java.time.Instant.ofEpochMilli(it)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate().format(dateFmt)
                            }
                            showEndDatePicker = false
                        }) { Text("确定") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) { Text("取消") }
                    }
                ) { DatePicker(state = state, title = { Text("结束日期") }) }
            }

            // ===== 闹钟设置 =====
            SectionCard(title = "闹钟提醒") {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用闹钟提醒", Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = enableAlarm, onCheckedChange = { enableAlarm = it })
                }
                if (enableAlarm) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = alarmMinutesBefore.toString(),
                            onValueChange = { alarmMinutesBefore = it.toIntOrNull() ?: 0 },
                            label = { Text("提前（分钟）") }, modifier = Modifier.weight(1f),
                            singleLine = true, shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = alarmRepeatInterval.toString(),
                            onValueChange = { alarmRepeatInterval = it.toIntOrNull() ?: 0 },
                            label = { Text("间隔（分钟）") }, modifier = Modifier.weight(1f),
                            singleLine = true, shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = alarmRepeatCount.toString(),
                            onValueChange = { alarmRepeatCount = it.toIntOrNull() ?: 0 },
                            label = { Text("重复次数") }, modifier = Modifier.weight(1f),
                            singleLine = true, shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // ===== 备注 =====
            SectionCard(title = "备注") {
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    placeholder = { Text("教室、教材等备注信息") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ===== 滑动滚轮时间选择器 =====
@Composable
private fun ScrollWheelTimeDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(value = hour, onValueChange = { hour = it },
                    range = 0..23, label = "时")
                Text(":", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 12.dp))
                WheelPicker(value = minute, onValueChange = { minute = it },
                    range = 0..59, label = "分")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ===== 时长滚轮选择器 =====
@Composable
private fun ScrollWheelDurationDialog(
    title: String,
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var duration by remember { mutableIntStateOf(initialValue) }
    val range = (15..180 step 15).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(value = duration, onValueChange = { duration = it },
                    range = range.first()..range.last(), label = "分钟",
                    customItems = range.map { it.toString() })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(duration) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 滑动滚轮选择器 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String,
    customItems: List<String>? = null
) {
    val labels = customItems ?: range.toList().map { "%02d".format(it) }
    val values = customItems?.mapNotNull { it.toIntOrNull() } ?: range.toList()
    val initialIndex = values.indexOf(value).coerceAtLeast(0) + 1
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerLazyIndex = remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val vc = info.viewportStartOffset + info.viewportSize.height / 2
            info.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs(item.offset + item.size / 2 - vc)
            }?.index ?: -1
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val realIndex = centerLazyIndex.value - 1
            val snapped = values.getOrElse(realIndex) { value }
            if (snapped != value) onValueChange(snapped)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            modifier = Modifier.height(160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(Modifier.height(56.dp)) }
            itemsIndexed(values) { index, item ->
                val isCenter = (index + 1) == centerLazyIndex.value
                val text = labels.getOrElse(index) { item.toString() }
                Text(
                    text = text,
                    style = if (isCenter) MaterialTheme.typography.headlineMedium
                        else MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCenter) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 12.dp)
                        .clickable { onValueChange(item) },
                    textAlign = TextAlign.Center
                )
            }
            item { Spacer(Modifier.height(56.dp)) }
        }
    }
}

/** 点击弹出选择器的假输入框 */
@Composable
private fun PickerTrigger(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp,
            MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** 星期多选芯片 */
@Composable
private fun DayChips(labels: List<String>, selected: Set<Int>, onToggle: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        labels.forEachIndexed { index, label ->
            val day = index + 1
            val sel = day in selected
            Surface(
                modifier = Modifier.weight(1f).clickable { onToggle(day) },
                shape = RoundedCornerShape(10.dp),
                color = if (sel) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(label, Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) { Column(Modifier.padding(16.dp), content = content) }
    }
}

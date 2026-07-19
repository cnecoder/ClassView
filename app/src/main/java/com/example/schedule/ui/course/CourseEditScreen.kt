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
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditScreen(
    course: Course,
    usedColors: Set<Int>,
    existingInstances: List<com.example.schedule.data.model.ClassInstance>,
    onSaveWithInstances: (Course, List<com.example.schedule.data.model.ClassInstance>) -> Unit,
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
    val selectedColor = availColors.getOrElse(selectedColorIdx) { availColors[0] }.hashCode()
    // 日历逐日覆盖数据（从已有实例重建）
    var dayOverrides by remember {
        mutableStateOf(reconstructOverrides(course, existingInstances))
    }

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
                        val saved = course.copy(
                            name = name, daysOfWeek = daysToStr(selectedDays),
                            startTime = startTime, endTime = computedEndTime,
                            startDate = startDate, endDate = endDate,
                            repeatWeeks = "", skipHolidays = skipHolidays,
                            enableAlarm = enableAlarm,
                            alarmMinutesBefore = alarmMinutesBefore,
                            alarmRepeatInterval = alarmRepeatInterval,
                            alarmRepeatCount = alarmRepeatCount,
                            color = selectedColor, note = note
                        )
                        val instances = buildInstances(saved, dayOverrides)
                        onSaveWithInstances(saved, instances)
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

            // ===== 课程日历预览 =====
            CalendarPreviewSection(
                startDate = startDate, endDate = endDate,
                daysOfWeek = daysToStr(selectedDays),
                skipHolidays = skipHolidays,
                startTime = startTime, durationMin = durationMin,
                enableAlarm = enableAlarm,
                alarmMinutesBefore = alarmMinutesBefore,
                alarmRepeatInterval = alarmRepeatInterval,
                alarmRepeatCount = alarmRepeatCount,
                selectedColor = selectedColor,
                overrides = dayOverrides,
                onOverridesChange = { dayOverrides = it }
            )

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

// ===== 课程日历预览 =====
data class DayOverride(
    val exclude: Boolean = false,
    val startTime: String? = null,
    val durationMin: Int? = null,
    val enableAlarm: Boolean? = null,
    val alarmMinutesBefore: Int? = null,
    val alarmRepeatInterval: Int? = null,
    val alarmRepeatCount: Int? = null
)

@Composable
private fun CalendarPreviewSection(
    startDate: String, endDate: String,
    daysOfWeek: String, skipHolidays: Boolean,
    startTime: String, durationMin: Int,
    enableAlarm: Boolean, alarmMinutesBefore: Int,
    alarmRepeatInterval: Int, alarmRepeatCount: Int,
    selectedColor: Int,
    overrides: Map<String, DayOverride>,
    onOverridesChange: (Map<String, DayOverride>) -> Unit
) {
    var currentMonth by remember { mutableStateOf(try { DateUtils.parseDate(startDate) } catch (_: Exception) { LocalDate.now() }.withDayOfMonth(1)) }
    var editTarget by remember { mutableStateOf<String?>(null) }
    var lastClickTime by remember { mutableStateOf(0L) }
    val fmt = DateTimeFormatter.ofPattern("yyyy年M月")
    val df = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 计算模板生成的默认活跃日期
    val defaultDates = remember(startDate, endDate, daysOfWeek) {
        val s = try { DateUtils.parseDate(startDate) } catch (_: Exception) { LocalDate.now() }
        val e = try { DateUtils.parseDate(endDate) } catch (_: Exception) { LocalDate.now() }
        val targets = DateUtils.parseDaySet(daysOfWeek)
        DateUtils.generateDatesByDayOfWeek(s, e, targets).map { DateUtils.formatDate(it) }.toSet()
    }

    val today = LocalDate.now()
    val currentMonthStr = currentMonth.format(df)

    // 弹窗编辑某天
    if (editTarget != null) {
        val date = editTarget!!
        val ov = overrides[date] ?: DayOverride()
        var editST by remember(date) { mutableStateOf(ov.startTime ?: startTime) }
        var editDur by remember(date) { mutableIntStateOf(ov.durationMin ?: durationMin) }
        var editAlarm by remember(date) { mutableStateOf(ov.enableAlarm ?: enableAlarm) }
        var editBefore by remember(date) { mutableIntStateOf(ov.alarmMinutesBefore ?: alarmMinutesBefore) }
        var editInterval by remember(date) { mutableIntStateOf(ov.alarmRepeatInterval ?: alarmRepeatInterval) }
        var editCount by remember(date) { mutableIntStateOf(ov.alarmRepeatCount ?: alarmRepeatCount) }
        var showEditTimePicker by remember { mutableStateOf(false) }
        var showEditDurPicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("$date 课程调整", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PickerTrigger(label = "开始时间", value = editST,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showEditTimePicker = true })
                    PickerTrigger(label = "时长", value = "${editDur}分钟",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showEditDurPicker = true })
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("启用闹钟", Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = editAlarm, onCheckedChange = { editAlarm = it })
                    }
                    if (editAlarm) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = editBefore.toString(),
                                onValueChange = { editBefore = it.toIntOrNull() ?: 0 },
                                label = { Text("提前") }, modifier = Modifier.weight(1f),
                                singleLine = true, shape = RoundedCornerShape(8.dp))
                            OutlinedTextField(value = editInterval.toString(),
                                onValueChange = { editInterval = it.toIntOrNull() ?: 0 },
                                label = { Text("间隔") }, modifier = Modifier.weight(1f),
                                singleLine = true, shape = RoundedCornerShape(8.dp))
                            OutlinedTextField(value = editCount.toString(),
                                onValueChange = { editCount = it.toIntOrNull() ?: 0 },
                                label = { Text("次数") }, modifier = Modifier.weight(1f),
                                singleLine = true, shape = RoundedCornerShape(8.dp))
                        }
                    }
                    if (showEditTimePicker) {
                        val (h, m) = editST.split(":").let {
                            (it.getOrNull(0)?.toIntOrNull() ?: 8) to (it.getOrNull(1)?.toIntOrNull() ?: 0)
                        }
                        ScrollWheelTimeDialog(title = "开始时间", initialHour = h, initialMinute = m,
                            onDismiss = { showEditTimePicker = false },
                            onConfirm = { hh, mm -> editST = "%02d:%02d".format(hh, mm); showEditTimePicker = false })
                    }
                    if (showEditDurPicker) {
                        ScrollWheelDurationDialog(title = "时长", initialValue = editDur,
                            onDismiss = { showEditDurPicker = false },
                            onConfirm = { editDur = it; showEditDurPicker = false })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newMap = overrides.toMutableMap()
                    newMap[date] = DayOverride(
                        exclude = false,
                        startTime = if (editST != startTime) editST else null,
                        durationMin = if (editDur != durationMin) editDur else null,
                        enableAlarm = if (editAlarm != enableAlarm) editAlarm else null,
                        alarmMinutesBefore = if (editBefore != alarmMinutesBefore) editBefore else null,
                        alarmRepeatInterval = if (editInterval != alarmRepeatInterval) editInterval else null,
                        alarmRepeatCount = if (editCount != alarmRepeatCount) editCount else null
                    )
                    onOverridesChange(newMap)
                    editTarget = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text("取消") }
            }
        )
    }

    SectionCard(title = "课程日历") {
        // 当月日历
        val firstDay = currentMonth.withDayOfMonth(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val startOffset = (firstDay.dayOfWeek.value - 1)
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        val weekHeaders = listOf("一", "二", "三", "四", "五", "六", "日")

        // 月份导航
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Text("◀", color = MaterialTheme.colorScheme.primary)
            }
            Text(currentMonth.format(fmt), style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Text("▶", color = MaterialTheme.colorScheme.primary)
            }
        }

        Row(Modifier.fillMaxWidth()) {
            weekHeaders.forEach { d ->
                Text(d, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(2.dp))

        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth().height(44.dp)) {
                for (col in 0..6) {
                    val cellIdx = row * 7 + col
                    val dayNum = cellIdx - startOffset + 1
                    if (dayNum in 1..daysInMonth) {
                        val date = currentMonth.withDayOfMonth(dayNum)
                        val dateStr = DateUtils.formatDate(date)
                        val isInRange = dateStr >= startDate && dateStr <= endDate
                        val isDefault = dateStr in defaultDates
                        val ov = overrides[dateStr]
                        val isExcluded = ov?.exclude == true
                        val isOverride = ov != null && !isExcluded
                        val isActive = (isDefault && !isExcluded) || isOverride
                        val isToday = date == today

                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp)
                                .clickable {
                                    val now = System.currentTimeMillis()
                                    if (now - lastClickTime < 400) {
                                        // 双击编辑
                                        if (isInRange) editTarget = dateStr
                                    } else {
                                        // 单击切换
                                        if (isInRange) {
                                            val newMap = overrides.toMutableMap()
                                            if (isDefault) {
                                                newMap[dateStr] = DayOverride(exclude = !isExcluded)
                                            } else if (!isActive) {
                                                newMap[dateStr] = DayOverride(exclude = false)
                                            } else {
                                                newMap[dateStr] = DayOverride(exclude = true)
                                            }
                                            onOverridesChange(newMap)
                                        }
                                    }
                                    lastClickTime = now
                                },
                            shape = RoundedCornerShape(4.dp),
                            color = when {
                                !isInRange -> Color.Transparent
                                isActive -> Color(selectedColor).copy(alpha = 0.25f)
                                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else -> Color.Transparent
                            },
                            border = if (isActive)
                                androidx.compose.foundation.BorderStroke(1.5.dp, Color(selectedColor))
                            else null
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("$dayNum",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (!isInRange) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        else if (isActive) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(10.dp), shape = CircleShape,
                    color = Color(selectedColor).copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedColor))) {}
                Spacer(Modifier.width(4.dp))
                Text("上课", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("单击切换 双击编辑", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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

/** 从已有实例反向重建覆盖映射 */
private fun reconstructOverrides(
    course: Course,
    existing: List<com.example.schedule.data.model.ClassInstance>
): Map<String, DayOverride> {
    if (existing.isEmpty()) return emptyMap()
    val result = mutableMapOf<String, DayOverride>()
    val instMap = existing.associateBy { it.date }

    // 生成模板日期
    val s = try { DateUtils.parseDate(course.startDate) } catch (_: Exception) { java.time.LocalDate.now() }
    val e = try { DateUtils.parseDate(course.endDate) } catch (_: Exception) { java.time.LocalDate.now() }
    val targets = DateUtils.parseDaySet(course.daysOfWeek)
    val allDates = DateUtils.generateDatesByDayOfWeek(s, e, targets)
    val defaultDur = DateUtils.timeToMinutes(course.endTime) - DateUtils.timeToMinutes(course.startTime)

    for (d in allDates) {
        val ds = DateUtils.formatDate(d)
        val inst = instMap[ds]
        if (inst == null) {
            // 模板有但实例表没有 → 被排除了
            result[ds] = DayOverride(exclude = true)
        } else if (inst.manuallyEdited) {
            // 单独调整过 → 记录差异
            val instDur = DateUtils.timeToMinutes(inst.endTime) - DateUtils.timeToMinutes(inst.startTime)
            result[ds] = DayOverride(
                exclude = false,
                startTime = if (inst.startTime != course.startTime) inst.startTime else null,
                durationMin = if (instDur != defaultDur) instDur else null,
                enableAlarm = if (inst.enableAlarm != course.enableAlarm) inst.enableAlarm else null,
                alarmMinutesBefore = if (inst.alarmMinutesBefore != course.alarmMinutesBefore) inst.alarmMinutesBefore else null,
                alarmRepeatInterval = if (inst.alarmRepeatInterval != course.alarmRepeatInterval) inst.alarmRepeatInterval else null,
                alarmRepeatCount = if (inst.alarmRepeatCount != course.alarmRepeatCount) inst.alarmRepeatCount else null
            )
        }
    }
    return result
}

// ===== 从模板 + 覆盖生成 ClassInstance 列表 =====
private fun buildInstances(
    course: Course,
    overrides: Map<String, DayOverride>
): List<com.example.schedule.data.model.ClassInstance> {
    val s = try { DateUtils.parseDate(course.startDate) } catch (_: Exception) { java.time.LocalDate.now() }
    val e = try { DateUtils.parseDate(course.endDate) } catch (_: Exception) { java.time.LocalDate.now() }
    val targets = DateUtils.parseDaySet(course.daysOfWeek)
    val allDates = DateUtils.generateDatesByDayOfWeek(s, e, targets)
    val defaultEnd = DateUtils.minutesToTime(DateUtils.timeToMinutes(course.startTime) + 90)

    val result = mutableListOf<com.example.schedule.data.model.ClassInstance>()
    for (date in allDates) {
        val ds = DateUtils.formatDate(date)

        val ov = overrides[ds]
        if (ov?.exclude == true) continue

        val st = ov?.startTime ?: course.startTime
        val dm = ov?.durationMin ?: DateUtils.timeToMinutes(course.endTime) - DateUtils.timeToMinutes(course.startTime)
        val et = DateUtils.minutesToTime(DateUtils.timeToMinutes(st) + dm.coerceIn(15, 180))

        val hasOverride = ov != null
        result.add(com.example.schedule.data.model.ClassInstance(
            courseId = course.id,
            date = ds,
            startTime = st,
            endTime = et,
            enableAlarm = ov?.enableAlarm ?: course.enableAlarm,
            alarmMinutesBefore = ov?.alarmMinutesBefore ?: course.alarmMinutesBefore,
            alarmRepeatInterval = ov?.alarmRepeatInterval ?: course.alarmRepeatInterval,
            alarmRepeatCount = ov?.alarmRepeatCount ?: course.alarmRepeatCount,
            manuallyEdited = hasOverride
        ))
    }
    return result
}

package com.example.schedule.ui.course

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.schedule.data.model.Course
import com.example.schedule.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    courses: List<Course>,
    instances: List<com.example.schedule.data.model.ClassInstance>,
    holidayMap: Map<String, Boolean>,
    themeKey: String,
    onThemeChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Course) -> Unit,
    onDeleteClick: (Course) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("本周课程", "日历视图", "课程列表")
    var showThemeMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("课程表", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        Box {
                            IconButton(onClick = { showThemeMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "切换主题",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showThemeMenu,
                                onDismissRequest = { showThemeMenu = false }
                            ) {
                                com.example.schedule.ui.theme.AllThemePresets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (preset.key == themeKey) {
                                                    Icon(Icons.Default.Check, null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(Modifier.width(8.dp))
                                                }
                                                Text(preset.label)
                                            }
                                        },
                                        onClick = {
                                            onThemeChange(preset.key)
                                            showThemeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                TabRow(selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick, shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(4.dp, 8.dp)
            ) { Icon(Icons.Default.Add, "添加课程") }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> WeekView(courses, instances, holidayMap, padding, onEditClick, onDeleteClick)
            1 -> CalendarView(courses, instances, holidayMap, padding, onEditClick)
            2 -> CourseListView(courses, instances, padding, onEditClick)
        }
    }
}

// ===== 本周课程视图 =====
@Composable
private fun WeekView(
    courses: List<Course>,
    instances: List<com.example.schedule.data.model.ClassInstance>,
    holidayMap: Map<String, Boolean>,
    padding: PaddingValues,
    onEditClick: (Course) -> Unit,
    onDeleteClick: (Course) -> Unit
) {
    var deleteConfirmCourse by remember { mutableStateOf<Course?>(null) }

    val today = LocalDate.now()
    val weekStart = today
    val weekEnd = today.plusDays(6)
    val weekStartStr = DateUtils.formatDate(weekStart)
    val weekEndStr = DateUtils.formatDate(weekEnd)

    // 筛选本周有课的实例
    val weekInstances = remember(instances) {
        instances.filter { it.date >= weekStartStr && it.date <= weekEndStr }
    }

    // 本周有实例的课程 ID 集合
    val weekCourseIds = remember(weekInstances) { weekInstances.map { it.courseId }.toSet() }
    val courseMap = remember(courses) { courses.associateBy { it.id } }

    if (weekInstances.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📅", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(16.dp))
                Text("本周暂无课程", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("点击右下角 + 添加课程", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    } else {
        // 按日期分组
        val groupedByDate = remember(weekInstances) {
            weekInstances.groupBy { it.date }
        }
        val weekLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val weekIcons = listOf("一", "二", "三", "四", "五", "六", "日")
        val fmt = DateTimeFormatter.ofPattern("MM/dd")

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 0..6) {
                val date = weekStart.plusDays(i.toLong())
                val dateStr = DateUtils.formatDate(date)
                val dayInstances = groupedByDate[dateStr] ?: emptyList()
                if (dayInstances.isNotEmpty()) {
                    val dow = date.dayOfWeek.value
                    item {
                        Row(
                            Modifier.padding(top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(weekIcons[dow - 1], color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(weekLabels[dow - 1],
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(date.format(fmt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    items(dayInstances, key = { "inst-${it.id}" }) { inst ->
                        val c = courseMap[inst.courseId]
                        if (c != null) {
                            CourseCard(c, inst, onEditClick,
                                onDelete = { deleteConfirmCourse = c })
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    deleteConfirmCourse?.let { course ->
        AlertDialog(
            onDismissRequest = { deleteConfirmCourse = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("确认删除", fontWeight = FontWeight.SemiBold) },
            text = { Text("确定要删除「${course.name}」吗？") },
            confirmButton = {
                TextButton(onClick = { onDeleteClick(course); deleteConfirmCourse = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmCourse = null }) { Text("取消") }
            }
        )
    }
}

// ===== 课程列表视图 =====
@Composable
private fun CourseListView(
    courses: List<Course>,
    instances: List<com.example.schedule.data.model.ClassInstance>,
    padding: PaddingValues,
    onEditClick: (Course) -> Unit
) {
    var selectedCourse by remember { mutableStateOf<Course?>(null) }

    if (selectedCourse != null) {
        // 展开某课程的日历详情
        val c = selectedCourse!!
        val courseInsts = remember(instances, c.id) {
            instances.filter { it.courseId == c.id }.sortedBy { it.date }
        }
        val fmt = DateTimeFormatter.ofPattern("yyyy年M月")
        val currentMonth = try { DateUtils.parseDate(c.startDate) } catch (_: Exception) { LocalDate.now() }

        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            // 标题行
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectedCourse = null }) {
                    Text("◀ 返回", color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.weight(1f))
                Text(c.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onEditClick(c) }) {
                    Text("编辑", color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold)
                }
            }

            // 课程模板信息
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Text("时间", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${c.startTime} - ${c.endTime}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column {
                        Text("日期", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${c.startDate} ~ ${c.endDate}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("${courseInsts.size} 节课", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)

            // 实例列表
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(courseInsts, key = { "cl-${it.id}" }) { inst ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (inst.manuallyEdited)
                                Color(c.color).copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(inst.date, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(90.dp))
                            Text("${inst.startTime} - ${inst.endTime}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold)
                            if (inst.manuallyEdited) {
                                Spacer(Modifier.weight(1f))
                                Text("已调整", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    } else {
        // 课程列表
        val fmt = DateTimeFormatter.ofPattern("MM/dd")
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (courses.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize().height(200.dp),
                        contentAlignment = Alignment.Center) {
                        Text("暂无课程", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(courses, key = { "list-${it.id}" }) { course ->
                Card(
                    Modifier.fillMaxWidth().clickable { selectedCourse = course },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(course.color).copy(alpha = 0.15f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(course.name, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text("开始时间", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(course.startTime, style = MaterialTheme.typography.bodyMedium)
                            }
                            val dur = DateUtils.timeToMinutes(course.endTime) - DateUtils.timeToMinutes(course.startTime)
                            Column {
                                Text("时长", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${dur}分钟", style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text("日期", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(course.startDate, style = MaterialTheme.typography.bodyMedium)
                                Text(course.endDate, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ===== 日历视图 =====
@Composable
private fun CalendarView(
    courses: List<Course>,
    instances: List<com.example.schedule.data.model.ClassInstance>,
    holidayMap: Map<String, Boolean>,
    padding: PaddingValues,
    onEditClick: (Course) -> Unit
) {
    var currentMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val courseMap = remember(courses) { courses.associateBy { it.id } }
    val fmt = DateTimeFormatter.ofPattern("yyyy年M月")

    // 按月计算实例
    val monthData = remember(instances, currentMonth) {
        val start = currentMonth
        val end = currentMonth.plusMonths(1).minusDays(1)
        instances.filter {
            it.date >= DateUtils.formatDate(start) && it.date <= DateUtils.formatDate(end)
        }.groupBy { it.date }
    }

    // 选中日课程实例
    val selectedInsts = remember(selectedDate, monthData) {
        if (selectedDate == null) emptyList()
        else monthData[DateUtils.formatDate(selectedDate!!)] ?: emptyList()
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        // 可滑动日历区域
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .pointerInput(currentMonth) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount < -50f) currentMonth = currentMonth.plusMonths(1)
                        else if (dragAmount > 50f) currentMonth = currentMonth.minusMonths(1)
                    }
                }
        ) {
            // 月份标题（居中）
            Text(
                currentMonth.format(fmt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )

            // 星期标题
            val weekHeaders = listOf("一", "二", "三", "四", "五", "六", "日")
            Row(Modifier.fillMaxWidth()) {
                weekHeaders.forEach { d ->
                    Text(d, Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))

            // 日历格子
            val firstDay = currentMonth.withDayOfMonth(1)
            val daysInMonth = currentMonth.lengthOfMonth()
            val startOffset = (firstDay.dayOfWeek.value - 1)
            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth().height(64.dp)) {
                    for (col in 0..6) {
                        val cellIdx = row * 7 + col
                        val dayNum = cellIdx - startOffset + 1

                        if (dayNum in 1..daysInMonth) {
                            val date = currentMonth.withDayOfMonth(dayNum)
                            val dateStr = DateUtils.formatDate(date)
                            val dayInsts = monthData[dateStr] ?: emptyList()
                            val isHoliday = holidayMap[dateStr] == true
                            val isToday = date == LocalDate.now()
                            val isSelected = date == selectedDate

                            Surface(
                                modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp)
                                    .clickable { selectedDate = date },
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    isHoliday -> Color(0xFFFFF3E0)
                                    else -> Color.Transparent
                                },
                                border = when {
                                    isSelected -> androidx.compose.foundation.BorderStroke(2.dp,
                                        MaterialTheme.colorScheme.primary)
                                    isToday -> androidx.compose.foundation.BorderStroke(1.5.dp,
                                        MaterialTheme.colorScheme.primary)
                                    else -> null
                                }
                            ) {
                                Column(Modifier.padding(2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$dayNum",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isToday || isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                        dayInsts.mapNotNull { courseMap[it.courseId] }.distinct().take(3).forEach { c ->
                                            Surface(Modifier.size(6.dp), shape = CircleShape,
                                                color = Color(c.color)) {}
                                        }
                                        if (dayInsts.isNotEmpty() && dayInsts.map { it.courseId }.distinct().size > 3) {
                                            Text("…", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (isHoliday && dayInsts.isEmpty()) {
                                        Text("休", style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFF9800))
                                    }
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }

            // 图例
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Surface(Modifier.size(8.dp), shape = CircleShape, color = Color(0xFFFFF3E0)) {}
                Spacer(Modifier.width(4.dp))
                Text("节假日", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Text("< 左右滑动切换月份 >", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 选中日课程详情
        if (selectedInsts.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "  ${selectedDate?.format(DateTimeFormatter.ofPattern("MM月dd日"))} 课程:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(4.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(selectedInsts, key = { "cal-${it.id}" }) { inst ->
                    val c = courseMap[inst.courseId]
                    if (c != null) {
                        CourseCard(c, inst, onEditClick, onDelete = {})
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun CourseCard(
    course: Course,
    instance: com.example.schedule.data.model.ClassInstance? = null,
    onClick: (Course) -> Unit,
    onDelete: () -> Unit
) {
    val courseColor = Color(course.color)
    val displayTime = if (instance != null) "${instance.startTime} - ${instance.endTime}" else "${course.startTime} - ${course.endTime}"
    val hasAlarm = instance?.enableAlarm ?: course.enableAlarm

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(course) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = courseColor.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(Modifier.size(10.dp), shape = CircleShape, color = courseColor) {}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(displayTime, style = MaterialTheme.typography.bodyMedium)
                if (instance?.manuallyEdited == true) {
                    Text("已单独调整", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Icon(
                imageVector = if (hasAlarm) Icons.Default.Notifications
                    else Icons.Default.NotificationsOff,
                contentDescription = null, modifier = Modifier.size(18.dp),
                tint = if (hasAlarm) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "删除", Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

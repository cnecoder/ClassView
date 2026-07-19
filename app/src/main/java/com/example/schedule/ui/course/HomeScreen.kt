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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
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

private fun courseDateLabel(course: Course): String {
    return try {
        val inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val outFmt = DateTimeFormatter.ofPattern("MM/dd")
        val s = LocalDate.parse(course.startDate, inFmt).format(outFmt)
        val e = LocalDate.parse(course.endDate, inFmt).format(outFmt)
        "$s - $e"
    } catch (_: Exception) { "" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    courses: List<Course>,
    holidayMap: Map<String, Boolean>,
    onAddClick: () -> Unit,
    onEditClick: (Course) -> Unit,
    onDeleteClick: (Course) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("本周课程", "日历视图")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("课程表", fontWeight = FontWeight.SemiBold) },
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
            0 -> WeekView(courses, holidayMap, padding, onEditClick, onDeleteClick)
            1 -> CalendarView(courses, holidayMap, padding, onEditClick)
        }
    }
}

// ===== 本周课程视图 =====
@Composable
private fun WeekView(
    courses: List<Course>,
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

    // 筛选本周有课的课程
    val weekCourses = remember(courses.hashCode(), holidayMap.hashCode()) {
        val filtered = courses.filter { course ->
            val classDates = com.example.schedule.util.CourseCalculator.calculate(course, holidayMap)
            val inWeek = classDates.any { cd ->
                val d = DateUtils.formatDate(cd.date)
                d >= weekStartStr && d <= weekEndStr
            }
            if (!inWeek) {
                com.example.schedule.util.DebugLog.w("Week", "[${course.name}] NO week=$weekStartStr~$weekEndStr days=${course.daysOfWeek} dates=${classDates.size}")
            }
            inWeek
        }
        com.example.schedule.util.DebugLog.w("Week", "total=${courses.size} filtered=${filtered.size} week=$weekStartStr~$weekEndStr holidayKeys=${holidayMap.size}")
        filtered
    }

    if (weekCourses.isEmpty()) {
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
        val expanded = mutableListOf<Pair<Int, Course>>()
        for (c in weekCourses) {
            val days = DateUtils.parseDaySet(c.daysOfWeek)
            for (d in days) { expanded.add(d to c) }
        }
        val grouped = expanded.groupBy({ it.first }, { it.second })
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
                val dow = date.dayOfWeek.value // 1=Mon..7=Sun
                val dayCourses = grouped[dow] ?: emptyList()
                if (dayCourses.isNotEmpty()) {
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
                    items(count = dayCourses.size) { idx ->
                        CourseCard(dayCourses[idx], onEditClick,
                            onDelete = { deleteConfirmCourse = dayCourses[idx] })
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

// ===== 日历视图 =====
@Composable
private fun CalendarView(
    courses: List<Course>,
    holidayMap: Map<String, Boolean>,
    padding: PaddingValues,
    onEditClick: (Course) -> Unit
) {
    var currentMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val fmt = DateTimeFormatter.ofPattern("yyyy年M月")

    // 按月计算课程
    val monthData = remember(courses.hashCode(), holidayMap.hashCode(), currentMonth) {
        val start = currentMonth
        val end = currentMonth.plusMonths(1).minusDays(1)
        val result = mutableMapOf<String, List<Course>>()
        for (c in courses) {
            val dates = com.example.schedule.util.CourseCalculator.calculate(c, holidayMap)
            for (cd in dates) {
                val ds = DateUtils.formatDate(cd.date)
                if (ds >= DateUtils.formatDate(start) && ds <= DateUtils.formatDate(end)) {
                    result[ds] = (result[ds] ?: emptyList()) + c
                }
            }
        }
        result
    }

    // 选中日课程
    val selectedCourses = remember(selectedDate, monthData) {
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
                            val dayCourses = monthData[dateStr] ?: emptyList()
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
                                        dayCourses.take(3).forEach { c ->
                                            Surface(Modifier.size(6.dp), shape = CircleShape,
                                                color = Color(c.color)) {}
                                        }
                                        if (dayCourses.size > 3) {
                                            Text("…", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (isHoliday && dayCourses.isEmpty()) {
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
        if (selectedCourses.isNotEmpty()) {
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
                items(selectedCourses, key = { "cal-${it.id}" }) { course ->
                    CourseCard(course, onEditClick, onDelete = {})
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
    onClick: (Course) -> Unit,
    onDelete: () -> Unit
) {
    val courseColor = Color(course.color)
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
                Text("${course.startTime} - ${course.endTime}",
                    style = MaterialTheme.typography.bodyMedium)
                Text(courseDateLabel(course), style = MaterialTheme.typography.bodySmall)
            }
            Icon(
                imageVector = if (course.enableAlarm) Icons.Default.Notifications
                    else Icons.Default.NotificationsOff,
                contentDescription = null, modifier = Modifier.size(18.dp),
                tint = if (course.enableAlarm) MaterialTheme.colorScheme.primary
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

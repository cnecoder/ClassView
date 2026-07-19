package com.example.schedule

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.schedule.ui.course.CourseEditScreen
import com.example.schedule.ui.course.HomeScreen
import com.example.schedule.ui.course.CourseViewModel
import com.example.schedule.ui.theme.ScheduleTheme
import com.example.schedule.ui.theme.ThemeManager
import com.example.schedule.util.DateUtils

class MainActivity : ComponentActivity() {

    private val viewModel: CourseViewModel by viewModels {
        CourseViewModel.Factory(application as ScheduleApp)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    private val alarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            var themeKey by remember { mutableStateOf(ThemeManager.getCurrentTheme(this)) }
            ScheduleTheme(themeKey = themeKey) {
                val courses by viewModel.courses.collectAsState()
                val instances by viewModel.instances.collectAsState()
                val holidayMap by viewModel.holidayMap.collectAsState()
                val editing by viewModel.editingCourse.collectAsState()

                if (editing != null) {
                    val usedColors = courses
                        .filter { it.id != editing!!.id && it.endDate >= DateUtils.todayStr() }
                        .map { it.color }.toSet()
                    val editInstances = instances.filter { it.courseId == editing!!.id }
                    CourseEditScreen(
                        course = editing!!,
                        usedColors = usedColors,
                        existingInstances = editInstances,
                        onSaveWithInstances = { course, insts ->
                            viewModel.saveCourseWithInstances(course, insts)
                        },
                        onCancel = { viewModel.cancelEdit() }
                    )
                } else {
                    HomeScreen(
                        courses = courses,
                        instances = instances,
                        holidayMap = holidayMap,
                        themeKey = themeKey,
                        onThemeChange = { key ->
                            ThemeManager.setCurrentTheme(this@MainActivity, key)
                            themeKey = key
                        },
                        onAddClick = { viewModel.startNewCourse() },
                        onEditClick = { viewModel.startEditCourse(it) },
                        onDeleteClick = { viewModel.deleteCourse(it) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 检查闹钟权限是否就绪
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                requestExactAlarmPermission()
            }
        }
    }

    private fun requestPermissions() {
        // 通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 精确闹钟权限 (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                requestExactAlarmPermission()
            }
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            alarmPermissionLauncher.launch(intent)
        }
    }
}

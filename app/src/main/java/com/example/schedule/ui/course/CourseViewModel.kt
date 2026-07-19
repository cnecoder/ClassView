package com.example.schedule.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.schedule.ScheduleApp
import com.example.schedule.data.model.Course
import com.example.schedule.data.repository.CourseRepository
import com.example.schedule.data.repository.HolidayRepository
import com.example.schedule.alarm.AlarmScheduler
import com.example.schedule.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CourseViewModel(
    private val courseRepo: CourseRepository,
    private val holidayRepo: HolidayRepository,
    private val app: ScheduleApp
) : ViewModel() {

    val courses: StateFlow<List<Course>> = courseRepo.getAllCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _holidayMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val holidayMap: StateFlow<Map<String, Boolean>> = _holidayMap.asStateFlow()

    private val _editingCourse = MutableStateFlow<Course?>(null)
    val editingCourse: StateFlow<Course?> = _editingCourse.asStateFlow()

    init {
        viewModelScope.launch {
            val today = DateUtils.today()
            val yr = today.year
            val start = "${yr}-01-01"
            val end = "${yr + 1}-12-31"
            _holidayMap.value = holidayRepo.getHolidayMap(start, end)
            com.example.schedule.util.DebugLog.w("VM", "VM: holidayMap loaded, size=${_holidayMap.value.size}")
        }
        viewModelScope.launch {
            courses.collect { list ->
                list.forEach { c ->
                    com.example.schedule.util.DebugLog.w("VM", "VM: course [${c.name}] id=${c.id} days=${c.daysOfWeek} range=${c.startDate}~${c.endDate} alarm=${c.enableAlarm} rest=${c.restDays}")
                }
            }
        }
    }

    fun startNewCourse() {
        val today = DateUtils.todayStr()
        _editingCourse.value = Course(
            name = "",
            daysOfWeek = "1",
            startTime = "08:00",
            endTime = "09:30",
            startDate = today,
            endDate = today,
            repeatWeeks = "",
            skipHolidays = true,
            restDays = "6,7",
            enableAlarm = false,
            alarmMinutesBefore = 15,
            alarmRepeatInterval = 5,
            alarmRepeatCount = 3,
            color = 0xFFB5C7D3.toInt(),
            note = ""
        )
    }

    fun startEditCourse(course: Course) {
        _editingCourse.value = course
    }

    fun cancelEdit() {
        _editingCourse.value = null
    }

    fun saveCourse(course: Course) {
        viewModelScope.launch {
            try {
                if (course.id == 0L) {
                    com.example.schedule.util.DebugLog.w("VM", "insert course: ${course.daysOfWeek}")
                    val id = courseRepo.insert(course)
                    val saved = course.copy(id = id)
                    com.example.schedule.util.DebugLog.w("VM", "inserted id=$id, scheduling alarm")
                    scheduleAlarm(saved)
                } else {
                    courseRepo.update(course)
                    scheduleAlarm(course)
                }
                _editingCourse.value = null
                com.example.schedule.util.DebugLog.w("VM", "save done")
            } catch (e: Exception) {
                com.example.schedule.util.DebugLog.e("VM", "save failed", e)
                _editingCourse.value = null
            }
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            val scheduler = AlarmScheduler(app, courseRepo)
            scheduler.cancelForCourse(course.id)
            courseRepo.delete(course)
        }
    }

    private suspend fun scheduleAlarm(course: Course) {
        val scheduler = AlarmScheduler(app, courseRepo)
        scheduler.scheduleForCourse(course, holidayRepo)

        // 将下一个闹钟添加到系统时钟 App
        val firstTime = scheduler.getFirstAlarmTime(course, holidayRepo)
        if (firstTime != null) {
            scheduler.addToSystemClock(course, firstTime)
        }
    }

    class Factory(private val app: ScheduleApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourseViewModel(app.courseRepository, app.holidayRepository, app) as T
        }
    }
}

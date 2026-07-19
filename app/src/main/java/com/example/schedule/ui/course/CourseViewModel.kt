package com.example.schedule.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.schedule.ScheduleApp
import com.example.schedule.data.model.ClassInstance
import com.example.schedule.data.model.Course
import com.example.schedule.data.repository.CourseRepository
import com.example.schedule.data.repository.HolidayRepository
import com.example.schedule.alarm.AlarmScheduler
import com.example.schedule.util.CourseCalculator
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

    val instances: StateFlow<List<ClassInstance>> = courseRepo.getAllInstances()
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
        }
    }

    fun startNewCourse() {
        val today = DateUtils.todayStr()
        _editingCourse.value = Course(
            name = "", daysOfWeek = "1",
            startTime = "08:00", endTime = "09:30",
            startDate = today, endDate = today,
            repeatWeeks = "", skipHolidays = true,
            enableAlarm = false,
            alarmMinutesBefore = 15, alarmRepeatInterval = 5, alarmRepeatCount = 3,
            color = 0xFFB5C7D3.toInt(), note = ""
        )
    }

    fun startEditCourse(course: Course) {
        _editingCourse.value = course
    }

    fun cancelEdit() {
        _editingCourse.value = null
    }

    fun saveCourseWithInstances(course: Course, instances: List<ClassInstance>) {
        viewModelScope.launch {
            try {
                if (course.id == 0L) {
                    val id = courseRepo.insert(course)
                    val saved = course.copy(id = id)
                    val withCourse = instances.map { it.copy(courseId = id) }
                    courseRepo.saveInstances(id, withCourse)
                    scheduleAlarm(saved)
                } else {
                    courseRepo.update(course)
                    courseRepo.saveInstances(course.id, instances)
                    scheduleAlarm(course)
                }
                _editingCourse.value = null
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

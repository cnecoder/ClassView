package com.example.schedule.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import com.example.schedule.data.model.Alarm
import com.example.schedule.data.model.ClassInstance
import com.example.schedule.data.model.Course
import com.example.schedule.data.repository.CourseRepository
import com.example.schedule.data.repository.HolidayRepository
import com.example.schedule.util.CourseCalculator
import com.example.schedule.util.DateUtils
import kotlinx.coroutines.flow.first

class AlarmScheduler(
    private val context: Context,
    private val courseRepo: CourseRepository
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** 为单个课程设置所有闹钟（从 ClassInstance 表读取） */
    suspend fun scheduleForCourse(
        course: Course,
        holidayRepo: HolidayRepository
    ) {
        cancelForCourse(course.id)

        if (!course.enableAlarm) return

        // 从 DB 读实例
        val instances = courseRepo.getAllInstances().first().filter { it.courseId == course.id }

        val timestamps = mutableListOf<Long>()
        for (inst in instances) {
            if (!inst.enableAlarm) continue
            val instTs = DateUtils.toTimestamp(
                DateUtils.parseDate(inst.date), inst.startTime
            )
            val firstAlarm = instTs - inst.alarmMinutesBefore * 60_000L
            timestamps.add(firstAlarm)
            for (i in 1..inst.alarmRepeatCount) {
                timestamps.add(firstAlarm + (inst.alarmRepeatInterval * i) * 60_000L)
            }
        }

        for (time in timestamps) {
            val a = Alarm(
                courseId = course.id,
                triggerTime = time,
                courseName = course.name,
                repeatIntervalMinutes = course.alarmRepeatInterval,
                repeatCount = course.alarmRepeatCount
            )
            val realId = courseRepo.insertAlarm(a)
            setSystemAlarm(a.copy(id = realId))
        }
    }

    private fun setSystemAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("course_name", alarm.courseName)
            putExtra("course_id", alarm.courseId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, alarm.id.toInt(), intent, flags)

        try {
            @Suppress("DEPRECATION")
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(alarm.triggerTime, pendingIntent), pendingIntent
            )
        } catch (e: SecurityException) {
            try { alarmManager.set(AlarmManager.RTC_WAKEUP, alarm.triggerTime, pendingIntent) }
            catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    suspend fun cancelForCourse(courseId: Long) {
        val existing = courseRepo.getAlarmsByCourse(courseId)
        for (alarm in existing) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, alarm.id.toInt(), intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            pi?.let { alarmManager.cancel(it); it.cancel() }
        }
    }

    suspend fun getFirstAlarmTime(course: Course, holidayRepo: HolidayRepository): Long? {
        val holidayMap = holidayRepo.getHolidayMap(course.startDate, course.endDate)
        val dates = CourseCalculator.calculate(course, holidayMap)
        val times = CourseCalculator.calculateAlarmTimes(dates, course.alarmMinutesBefore, 0, 0)
        val now = System.currentTimeMillis()
        return times.firstOrNull { it > now }
    }

    fun addToSystemClock(course: Course, triggerTime: Long) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, DateUtils.timestampToHour(triggerTime))
            putExtra(AlarmClock.EXTRA_MINUTES, DateUtils.timestampToMinute(triggerTime))
            putExtra(AlarmClock.EXTRA_MESSAGE, "课程提醒: ${course.name}")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun rescheduleAll(holidayRepo: HolidayRepository) {
        val courses = courseRepo.getCoursesWithAlarm()
        for (c in courses) {
            try { scheduleForCourse(c, holidayRepo) } catch (_: Exception) {}
        }
    }
}

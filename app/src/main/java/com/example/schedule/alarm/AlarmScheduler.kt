package com.example.schedule.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import com.example.schedule.data.model.Alarm
import com.example.schedule.data.model.Course
import com.example.schedule.data.repository.CourseRepository
import com.example.schedule.data.repository.HolidayRepository
import com.example.schedule.util.CourseCalculator
import com.example.schedule.util.DateUtils

class AlarmScheduler(
    private val context: Context,
    private val courseRepo: CourseRepository
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** 为单个课程设置所有闹钟 */
    suspend fun scheduleForCourse(
        course: Course,
        holidayRepo: HolidayRepository
    ) {
        cancelForCourse(course.id)

        if (!course.enableAlarm) return

        val holidayMap = holidayRepo.getHolidayMap(course.startDate, course.endDate)
        val classDates = CourseCalculator.calculate(course, holidayMap)

        val alarmTimes = CourseCalculator.calculateAlarmTimes(
            classDates = classDates,
            minutesBefore = course.alarmMinutesBefore,
            repeatIntervalMinutes = course.alarmRepeatInterval,
            repeatCount = course.alarmRepeatCount
        )

        // 先创建不带 id 的闹钟列表
        val alarmsToInsert = alarmTimes.map { time ->
            Alarm(
                courseId = course.id,
                triggerTime = time,
                courseName = course.name,
                repeatIntervalMinutes = course.alarmRepeatInterval,
                repeatCount = course.alarmRepeatCount
            )
        }

        // 逐个插入并立即拿到真实 id，设置系统闹钟
        for (alarm in alarmsToInsert) {
            val realId = courseRepo.insertAlarm(alarm)
            val saved = alarm.copy(id = realId)
            setSystemAlarm(saved)
        }
    }

    private fun setSystemAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("course_name", alarm.courseName)
            putExtra("course_id", alarm.courseId)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            flags
        )

        try {
            @Suppress("DEPRECATION")
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(alarm.triggerTime, pendingIntent),
                pendingIntent
            )
            com.example.schedule.util.DebugLog.w("Alarm", "set: ${alarm.courseName} @ ${alarm.triggerTime}")
        } catch (e: SecurityException) {
            com.example.schedule.util.DebugLog.w("Alarm", "no permission, fallback inexact")
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarm.triggerTime, pendingIntent)
            } catch (e2: Exception) {
                com.example.schedule.util.DebugLog.e("Alarm", "set failed completely", e2)
            }
        } catch (e: Exception) {
            com.example.schedule.util.DebugLog.e("Alarm", "set failed", e)
        }
    }

    /** 取消单个课程的所有闹钟 */
    suspend fun cancelForCourse(courseId: Long) {
        val existingAlarms = courseRepo.getAlarmsByCourse(courseId)
        for (alarm in existingAlarms) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    /** 计算课程的下一个闹钟时间，供 UI 调用 */
    suspend fun getFirstAlarmTime(course: Course, holidayRepo: HolidayRepository): Long? {
        if (!course.enableAlarm) return null
        val holidayMap = holidayRepo.getHolidayMap(course.startDate, course.endDate)
        val classDates = CourseCalculator.calculate(course, holidayMap)
        val alarmTimes = CourseCalculator.calculateAlarmTimes(
            classDates = classDates,
            minutesBefore = course.alarmMinutesBefore,
            repeatIntervalMinutes = course.alarmRepeatInterval,
            repeatCount = 0 // 只取首次提醒，不加重复
        )
        val now = System.currentTimeMillis()
        return alarmTimes.firstOrNull { it > now }
    }

    /** 将下一个闹钟添加到系统时钟 App */
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

    /** 重设所有课程的闹钟 */
    suspend fun rescheduleAll(holidayRepo: HolidayRepository) {
        val courses = courseRepo.getCoursesWithAlarm()
        for (course in courses) {
            try {
                scheduleForCourse(course, holidayRepo)
            } catch (_: Exception) {
                // 单个课程失败不影响其他课程
            }
        }
    }
}

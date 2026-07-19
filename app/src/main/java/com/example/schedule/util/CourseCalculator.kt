package com.example.schedule.util

import com.example.schedule.data.model.Course
import java.time.LocalDate

object CourseCalculator {

    data class ClassDate(
        val date: LocalDate,
        val dateStr: String,
        val weekNumber: Int,
        val timestamp: Long
    )

    data class DayOverrideData(
        val exclude: Boolean = false,
        val startTime: String? = null
    )

    fun calculate(course: Course, holidayMap: Map<String, Boolean>): List<ClassDate> {
        return calculate(course, holidayMap, emptyMap())
    }

    fun calculate(
        course: Course,
        holidayMap: Map<String, Boolean>,
        overrides: Map<String, DayOverrideData>
    ): List<ClassDate> {
        val startDate = DateUtils.parseDate(course.startDate)
        val endDate = DateUtils.parseDate(course.endDate)
        val targetDays = DateUtils.parseDaySet(course.daysOfWeek)
        if (targetDays.isEmpty()) return emptyList()

        val allDates = DateUtils.generateDatesByDayOfWeek(startDate, endDate, targetDays)
        val targetWeeks = DateUtils.parseWeekRanges(course.repeatWeeks)

        return allDates.mapNotNull { date ->
            val dateStr = DateUtils.formatDate(date)
            val weekNum = DateUtils.getSemesterWeek(date, startDate)

            if (course.skipHolidays) {
                val isHoliday = holidayMap[dateStr]
                if (isHoliday == true) return@mapNotNull null
            }

            if (targetWeeks.isNotEmpty() && weekNum !in targetWeeks) {
                return@mapNotNull null
            }

            // 应用逐日覆盖
            val ov = overrides[dateStr]
            if (ov?.exclude == true) return@mapNotNull null
            val effectiveTime = ov?.startTime ?: course.startTime

            ClassDate(
                date = date,
                dateStr = dateStr,
                weekNumber = weekNum,
                timestamp = DateUtils.toTimestamp(date, effectiveTime)
            )
        }
    }

    fun calculateAlarmTimes(
        classDates: List<ClassDate>,
        minutesBefore: Int,
        repeatIntervalMinutes: Int,
        repeatCount: Int
    ): List<Long> {
        val result = mutableListOf<Long>()
        for (cd in classDates) {
            val firstAlarm = cd.timestamp - minutesBefore * 60_000L
            result.add(firstAlarm)
            for (i in 1..repeatCount) {
                result.add(firstAlarm + (repeatIntervalMinutes * i) * 60_000L)
            }
        }
        return result.sorted()
    }
}

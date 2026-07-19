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

    fun calculate(course: Course, holidayMap: Map<String, Boolean>): List<ClassDate> {
        val startDate = DateUtils.parseDate(course.startDate)
        val endDate = DateUtils.parseDate(course.endDate)

        val targetDays = DateUtils.parseDaySet(course.daysOfWeek)
        com.example.schedule.util.DebugLog.w("Calc", "[${course.name}] days=$targetDays rest=${course.restDays} range=${course.startDate}~${course.endDate} holidays=${holidayMap.size}")

        if (targetDays.isEmpty()) {
            com.example.schedule.util.DebugLog.w("Calc", "[${course.name}] -> EMPTY (no days)")
            return emptyList()
        }

        val allDates = DateUtils.generateDatesByDayOfWeek(startDate, endDate, targetDays)
        val restDays = DateUtils.parseDaySet(course.restDays)
        val targetWeeks = DateUtils.parseWeekRanges(course.repeatWeeks)

        com.example.schedule.util.DebugLog.w("Calc", "[${course.name}] rawDates=${allDates.size} restDays=$restDays targetWeeks=$targetWeeks")

        return allDates.mapNotNull { date ->
            val dateStr = DateUtils.formatDate(date)
            val dayOfWeek = DateUtils.getDayOfWeek(date)
            val weekNum = DateUtils.getSemesterWeek(date, startDate)

            if (dayOfWeek in restDays) return@mapNotNull null

            if (course.skipHolidays) {
                val isHoliday = holidayMap[dateStr]
                if (isHoliday == true) return@mapNotNull null
            }

            if (targetWeeks.isNotEmpty() && weekNum !in targetWeeks) {
                return@mapNotNull null
            }

            ClassDate(
                date = date,
                dateStr = dateStr,
                weekNumber = weekNum,
                timestamp = DateUtils.toTimestamp(date, course.startTime)
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

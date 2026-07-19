package com.example.schedule.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields

object DateUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun parseDate(dateStr: String): LocalDate = LocalDate.parse(dateStr, dateFormatter)

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun today(): LocalDate = LocalDate.now()

    fun todayStr(): String = formatDate(today())

    fun currentYear(): Int = today().year

    fun getDayOfWeek(date: LocalDate): Int = date.dayOfWeek.value

    /** 解析逗号分隔的数字字符串 "1,3,5" -> Set<Int> */
    fun parseDaySet(str: String): Set<Int> {
        if (str.isBlank()) return emptySet()
        return str.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    /** 生成日期范围内匹配多个星期几的日期 */
    fun generateDatesByDayOfWeek(
        start: LocalDate,
        end: LocalDate,
        targetDays: Set<Int>
    ): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        var current = start
        while (!current.isAfter(end)) {
            if (getDayOfWeek(current) in targetDays) {
                result.add(current)
            }
            current = current.plusDays(1)
        }
        return result
    }

    /** 计算教学周次 */
    fun getSemesterWeek(date: LocalDate, semesterStart: LocalDate): Int {
        val weekFields = WeekFields.of(DayOfWeek.MONDAY, 4)
        val startWeek = semesterStart.get(weekFields.weekOfWeekBasedYear())
        val dateWeek = date.get(weekFields.weekOfWeekBasedYear())
        val startYear = semesterStart.year
        val dateYear = date.year
        return if (dateYear == startYear) {
            dateWeek - startWeek + 1
        } else {
            val endOfStartYear = LocalDate.of(startYear, 12, 31)
            val weeksInStartYear = endOfStartYear.get(weekFields.weekOfWeekBasedYear())
            (weeksInStartYear - startWeek + 1) + dateWeek
        }
    }

    /** 解析周次字符串 "1,2,3,5-18" -> Set<Int> */
    fun parseWeekRanges(weeksStr: String): Set<Int> {
        if (weeksStr.isBlank()) return emptySet()
        val result = mutableSetOf<Int>()
        weeksStr.split(",").forEach { part ->
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val range = trimmed.split("-")
                val start = range[0].toIntOrNull()
                val end = range[1].toIntOrNull()
                if (start != null && end != null && start <= end) {
                    for (i in start..end) result.add(i)
                }
            } else {
                trimmed.toIntOrNull()?.let { result.add(it) }
            }
        }
        return result
    }

    fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    fun minutesToTime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }

    fun toTimestamp(date: LocalDate, time: String): Long {
        val minutes = timeToMinutes(time)
        return date.atStartOfDay()
            .plusMinutes(minutes.toLong())
            .toEpochSecond(java.time.ZoneOffset.of("+8")) * 1000
    }

    fun timestampToHour(timestamp: Long): Int {
        return java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault()).hour
    }

    fun timestampToMinute(timestamp: Long): Int {
        return java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault()).minute
    }
}

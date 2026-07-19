package com.example.schedule.data.repository

import com.example.schedule.data.db.AlarmDao
import com.example.schedule.data.db.CourseDao
import com.example.schedule.data.model.Alarm
import com.example.schedule.data.model.Course
import kotlinx.coroutines.flow.Flow

class CourseRepository(
    private val courseDao: CourseDao,
    private val alarmDao: AlarmDao
) {

    fun getAllCourses(): Flow<List<Course>> = courseDao.getAllCourses()

    suspend fun getCourseById(id: Long): Course? = courseDao.getCourseById(id)

    suspend fun insert(course: Course): Long = courseDao.insert(course)

    suspend fun update(course: Course) = courseDao.update(course)

    suspend fun delete(course: Course) {
        courseDao.delete(course)
        alarmDao.deleteByCourse(course.id)
    }

    suspend fun deleteById(id: Long) {
        courseDao.deleteById(id)
        alarmDao.deleteByCourse(id)
    }

    suspend fun getCoursesWithAlarm(): List<Course> = courseDao.getCoursesWithAlarm()

    suspend fun saveAlarms(alarms: List<Alarm>) = alarmDao.insertAll(alarms)

    suspend fun insertAlarm(alarm: Alarm): Long = alarmDao.insert(alarm)

    suspend fun getAlarmsByCourse(courseId: Long): List<Alarm> = alarmDao.getAlarmsByCourse(courseId)

    suspend fun getAllAlarms(): List<Alarm> = alarmDao.getAllAlarms()

    suspend fun deleteExpiredAlarms(timestamp: Long) = alarmDao.deleteExpired(timestamp)
}

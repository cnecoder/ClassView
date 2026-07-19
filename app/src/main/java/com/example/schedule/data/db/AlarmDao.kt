package com.example.schedule.data.db

import androidx.room.*
import com.example.schedule.data.model.Alarm

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms WHERE courseId = :courseId")
    suspend fun getAlarmsByCourse(courseId: Long): List<Alarm>

    @Query("SELECT * FROM alarms ORDER BY triggerTime")
    suspend fun getAllAlarms(): List<Alarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alarms: List<Alarm>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: Alarm): Long

    @Query("DELETE FROM alarms WHERE courseId = :courseId")
    suspend fun deleteByCourse(courseId: Long)

    @Query("DELETE FROM alarms WHERE triggerTime < :timestamp")
    suspend fun deleteExpired(timestamp: Long)
}

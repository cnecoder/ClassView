package com.example.schedule.data.db

import androidx.room.*
import com.example.schedule.data.model.ClassInstance
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassInstanceDao {

    @Query("SELECT * FROM class_instances WHERE courseId = :courseId ORDER BY date, startTime")
    fun getByCourse(courseId: Long): Flow<List<ClassInstance>>

    @Query("SELECT * FROM class_instances WHERE date BETWEEN :from AND :to ORDER BY date, startTime")
    fun getByDateRange(from: String, to: String): Flow<List<ClassInstance>>

    @Query("SELECT * FROM class_instances WHERE date = :date ORDER BY startTime")
    suspend fun getByDate(date: String): List<ClassInstance>

    @Query("SELECT * FROM class_instances ORDER BY date, startTime")
    fun getAll(): Flow<List<ClassInstance>>

    @Query("SELECT ci.* FROM class_instances ci INNER JOIN courses c ON ci.courseId = c.id ORDER BY ci.date, ci.startTime")
    fun getAllActive(): Flow<List<ClassInstance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(instances: List<ClassInstance>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(instance: ClassInstance): Long

    @Update
    suspend fun update(instance: ClassInstance)

    @Query("DELETE FROM class_instances WHERE courseId = :courseId")
    suspend fun deleteByCourse(courseId: Long)

    @Query("DELETE FROM class_instances")
    suspend fun deleteAll()
}

package com.example.schedule.data.db

import androidx.room.*
import com.example.schedule.data.model.Holiday

@Dao
interface HolidayDao {

    @Query("SELECT * FROM holidays WHERE date = :date")
    suspend fun getHoliday(date: String): Holiday?

    @Query("SELECT * FROM holidays WHERE date BETWEEN :start AND :end")
    suspend fun getHolidaysBetween(start: String, end: String): List<Holiday>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holidays: List<Holiday>)

    @Query("SELECT COUNT(*) FROM holidays")
    suspend fun count(): Int

    @Query("DELETE FROM holidays")
    suspend fun deleteAll()
}

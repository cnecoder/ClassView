package com.example.schedule.data.repository

import com.example.schedule.data.db.HolidayDao
import com.example.schedule.data.model.Holiday
import com.example.schedule.data.remote.HolidayApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HolidayRepository(
    private val holidayDao: HolidayDao,
    private val holidayApi: HolidayApi
) {

    /** 获取日期范围内的节假日映射 date("yyyy-MM-dd") -> isOffDay */
    suspend fun getHolidayMap(start: String, end: String): Map<String, Boolean> {
        return withContext(Dispatchers.IO) {
            val cached = holidayDao.getHolidaysBetween(start, end)
            if (cached.isNotEmpty()) {
                cached.associate { it.date to it.isOffDay }
            } else {
                emptyMap()
            }
        }
    }

    /** 从 API 更新节假日数据 */
    suspend fun fetchAndCacheHolidays(years: List<Int>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val allHolidays = mutableListOf<Holiday>()
                for (year in years) {
                    val response = holidayApi.getYearHolidays(year)
                    if (response.code == 0 && response.holiday != null) {
                        for ((key, info) in response.holiday) {
                            if (info.holiday) {
                                allHolidays.add(
                                    Holiday(
                                        date = "${year}-$key",
                                        name = info.name,
                                        isOffDay = true
                                    )
                                )
                            }
                        }
                    }
                }
                if (allHolidays.isNotEmpty()) {
                    holidayDao.insertAll(allHolidays)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** 检查本地是否已有缓存 */
    suspend fun hasCache(): Boolean = holidayDao.count() > 0
}

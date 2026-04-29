package com.example.extensaotelas.BancoDeDados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sensorData: SensorDataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sensorDataList: List<SensorDataEntity>)

    @Query("SELECT * FROM sensor_data ORDER BY timestamp ASC")
    fun getAllSensorDataFlow(): Flow<List<SensorDataEntity>>

    @Query("SELECT MAX(timestamp) FROM sensor_data")
    suspend fun getMaxTimestamp(): Long?
}

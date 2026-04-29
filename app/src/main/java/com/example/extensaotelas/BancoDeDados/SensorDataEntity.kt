package com.example.extensaotelas.BancoDeDados

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_data")
data class SensorDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long, // Unix timestamp em segundos ou milissegundos
    val temperature: Float,
    val airHumidity: Float,
    val soilHumidity: Int
)

package com.example.extensaotelas

// Representa uma única leitura dos sensores
data class SensorData(
    val temperature: Float,
    val airHumidity: Float,
    val soilHumidity: Int
)

// Representa um agendamento da EEPROM do Arduino
data class Schedule(
    val index: Int,
    var startHour: Int,
    var startMinute: Int,
    var endHour: Int,
    var endMinute: Int,
    var daysFlags: Int // O valor 42 (Seg+Qua+Sex), por exemplo
)

// Representa o status da conexão Bluetooth
enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR, RECONNECT_FAILED
}
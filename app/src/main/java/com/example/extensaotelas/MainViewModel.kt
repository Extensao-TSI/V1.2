package com.example.extensaotelas

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Instância única do manager para todo o app
    val bluetoothManager = ArduinoBluetoothManager.getInstance(application)

    val sensorData: StateFlow<SensorData?> = bluetoothManager.sensorData
    val schedules: StateFlow<List<Schedule>> = bluetoothManager.schedules
    val connectionStatus: StateFlow<ConnectionStatus> = bluetoothManager.connectionStatus
    val timeData = bluetoothManager.timeData

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            bluetoothManager.connect(device)
        }
    }

    fun disconnect() {
        bluetoothManager.disconnect()
    }

    fun fetchSchedules() {
        bluetoothManager.requestScheduleList()
    }

    fun deleteSchedule(index: Int) {
        bluetoothManager.deleteSchedule(index)
        // Pede a lista atualizada para garantir a sincronia
        viewModelScope.launch {
            kotlinx.coroutines.delay(500) // Espera o Arduino processar
            fetchSchedules()
        }
    }

    fun saveSchedule(schedule: Schedule) {
        bluetoothManager.createOrUpdateSchedule(schedule)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            fetchSchedules()
        }
    }

    fun requestStatus() {
        bluetoothManager.requestStatusOnce()
    }

    fun requestTime() {
        bluetoothManager.requestTime()
    }

    fun syncRtc() {
        bluetoothManager.syncRtcWithPhoneTime()
    }

    // Dentro da classe MainViewModel

    fun reconnect() {
        bluetoothManager.reconnect()
    }
}
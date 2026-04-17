package com.example.extensaotelas

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.write

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

    private val _isManualModeActive = MutableStateFlow(false) // Começa como desligado
    val isManualModeActive = _isManualModeActive.asStateFlow()

    /**
     * Alterna o estado do modo manual e envia o comando correspondente.
     * Esta é a ÚNICA função que as Activities devem chamar para isso.
     */
    fun toggleManualMode() {
        // Inverte o valor atual do estado
        val newState = !_isManualModeActive.value
        _isManualModeActive.value = newState

        // Envia o comando correto baseado no novo estado
        if (newState) {
            // Se o novo estado é ATIVO, envia "M"
            bluetoothManager.activeManualMode()
        } else {
            // Se o novo estado é INATIVO, envia "A"
            bluetoothManager.disableManualMode()
        }
    }
}
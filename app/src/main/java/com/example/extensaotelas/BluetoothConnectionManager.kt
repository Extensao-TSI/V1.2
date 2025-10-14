package com.example.extensaotelas

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object BluetoothConnectionManager {

    private val _isConnected = MutableLiveData<Boolean>(false) // Começa como desconectado

     val isConnected: LiveData<Boolean> = _isConnected

    fun setConnectionStatus(connected: Boolean) {
        Log.d("BT_DEBUG", "BluetoothConnectionManager: setConnectionStatus chamado. Novo estado = $connected")
        _isConnected.postValue(connected)
    }
}
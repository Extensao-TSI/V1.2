package com.example.extensaotelas

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

@SuppressLint("MissingPermission")
class ArduinoBluetoothManager private constructor(context: Context) {

    companion object {
        // A anotação @Volatile garante que a instância é sempre lida da memória principal
        @Volatile
        private var INSTANCE: ArduinoBluetoothManager? = null

        // Esta é a função que as outras partes da app irão chamar para obter a instância única
        fun getInstance(context: Context): ArduinoBluetoothManager {
            // synchronized garante que apenas uma thread pode criar a instância, evitando duplicações
            return INSTANCE ?: synchronized(this) {
                val instance = ArduinoBluetoothManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData = _sensorData.asStateFlow()

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules = _schedules.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _lastCommand = MutableStateFlow("")
    private val _timeData = MutableStateFlow<IntArray?>(null) // year,month,day,hour,minute,second
    val timeData = _timeData.asStateFlow()

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var lastConnectedDevice: BluetoothDevice? = null

    fun connect(device: BluetoothDevice) {
        scope.launch {
            _connectionStatus.value = ConnectionStatus.CONNECTING

            // Cancelar discovery se estiver ativo — consome recursos da antena BT
            bluetoothAdapter?.cancelDiscovery()

            try {
                // Tentativa 1: conexão padrão via UUID SPP
                socket = device.createRfcommSocketToServiceRecord(sppUuid)
                socket?.connect()
            } catch (e: IOException) {
                // Tentativa 2: fallback via reflexão (necessário para muitos módulos HC-05/HC-06)
                android.util.Log.w("BT_DEBUG", "Conexão padrão falhou, tentando fallback: ${e.message}")
                try {
                    socket?.close()
                } catch (_: IOException) {}

                try {
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    socket = method.invoke(device, 1) as BluetoothSocket
                    socket?.connect()
                } catch (e2: Exception) {
                    android.util.Log.e("BT_DEBUG", "Fallback também falhou: ${e2.message}")
                    _connectionStatus.value = ConnectionStatus.ERROR
                    disconnect()
                    return@launch
                }
            } catch (e: Exception) {
                android.util.Log.e("BT_DEBUG", "Erro inesperado na conexão: ${e.message}")
                _connectionStatus.value = ConnectionStatus.ERROR
                disconnect()
                return@launch
            }

            try {
                inputStream = socket?.inputStream
                outputStream = socket?.outputStream
                _connectionStatus.value = ConnectionStatus.CONNECTED

                lastConnectedDevice = device // Guarda o dispositivo após conectar com sucesso

                startListening()
            } catch (e: Exception) {
                android.util.Log.e("BT_DEBUG", "Erro ao obter streams: ${e.message}")
                _connectionStatus.value = ConnectionStatus.ERROR
                disconnect()
            }
        }
    }

    // 3. Adicione esta nova função para tentar reconectar
    fun reconnect() {
        lastConnectedDevice?.let { device ->
            // Só tenta reconectar se a conexão não estiver já ativa ou em processo
            if (_connectionStatus.value != ConnectionStatus.CONNECTED && _connectionStatus.value != ConnectionStatus.CONNECTING) {
                connect(device)
            }
        }
    }

    // Dentro de ArduinoBluetoothManager.kt

    private fun startListening() {
        scope.launch {
            val reader = inputStream?.bufferedReader()
            while (isActive) {
                try {
                    val line = reader?.readLine()
                    if (line != null) {
                        // Adiciona um log para ver os dados brutos que chegam do Arduino
                        android.util.Log.d("BT_RAW_DATA", "Recebido: $line")
                        processResponse(line)
                    } else {
                        // Se a linha for nula, a conexão provavelmente foi perdida
                        android.util.Log.w("BT_DEBUG", "readLine() retornou nulo. A conexão pode ter sido fechada.")
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        break
                    }
                } catch (e: IOException) {
                    // Erro de conexão, como o dispositivo a ser desligado
                    android.util.Log.e("BT_DEBUG", "IOException no startListening: ${e.message}")
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    break
                } catch (e: Exception) {
                    // --- CAPTURA QUALQUER OUTRO ERRO ---
                    // Impede o crash da aplicação em caso de dados mal formatados ou outros problemas
                    android.util.Log.e("BT_DEBUG", "Erro inesperado ao processar dados: ${e.message}")
                    // Continua a ouvir, ignorando a linha que causou o problema
                }
            }
        }
    }

    private val scheduleListBuilder = mutableListOf<Schedule>()

    private fun processResponse(line: String) {
        val parts = line.split(":", limit = 2)
        if (parts.size < 2) return
        val prefix = parts[0]
        val payload = parts[1].trim()
        val data = payload.split(",")

        when (prefix) {
            "STATUS" -> {
                if (data.size == 3) {
                    try {
                        _sensorData.value = SensorData(data[0].toFloat(), data[1].toFloat(), data[2].toInt())
                    } catch (e: NumberFormatException) {}
                }
            }
            "OK" -> {
                // Auto-refresh de lista ao detectar mudanças no EEPROM
                run {
                    val pl = payload.lowercase()
                    if (pl.startsWith("agendamento salvo") || pl.startsWith("agendamento apagado")) {
                        // Arduino confirmou alteração: solicitar listagem atualizada (após pequeno atraso)
                        scope.launch {
                            delay(150)
                            requestScheduleList()
                        }
                        return
                    }
                }

                when {
                    // Resposta do comando T (hora): OK:yyyy,MM,dd,HH,mm,ss
                    data.size == 6 && _lastCommand.value.startsWith("T") -> {
                        try {
                            val arr = IntArray(6) { i -> data[i].toInt() }
                            _timeData.value = arr
                        } catch (_: NumberFormatException) { }
                    }
                    // Respostas de listagem/CRUD de agendamentos: OK:index,hI,mI,hF,mF,dias
                    data.size == 6 -> {
                        try {
                            scheduleListBuilder.add(
                                Schedule(
                                    data[0].toInt(),
                                    data[1].toInt(),
                                    data[2].toInt(),
                                    data[3].toInt(),
                                    data[4].toInt(),
                                    data[5].toInt()
                                )
                            )
                        } catch (_: NumberFormatException) { }
                    }
                }
            }
            "DONE" -> {
                _schedules.value = scheduleListBuilder.toList()
                scheduleListBuilder.clear()
            }
            "ERROR" -> {
                println("Erro do Arduino: $payload")
            }
        }
    }

    // Dentro de ArduinoBluetoothManager.kt

    fun sendCommand(command: String) {
        // Adicionado log para sabermos que a função foi chamada
        android.util.Log.d("BT_DEBUG", "Tentando enviar comando: $command")

        if (socket?.isConnected == true) {
            if (outputStream == null) {
                android.util.Log.e("BT_DEBUG", "ERRO: outputStream é nulo. O comando não pode ser enviado.")
                return
            }
            scope.launch {
                try {
                    _lastCommand.value = command
                    outputStream?.write((command + "\n").toByteArray())
                    outputStream?.flush()
                    // Log de sucesso
                    android.util.Log.i("BT_DEBUG", "Comando '$command' enviado com SUCESSO.")
                } catch (e: IOException) {
                    // Log de erro
                    android.util.Log.e("BT_DEBUG", "ERRO de IO ao enviar comando '$command': ${e.message}")
                    // Tenta reconectar ou notificar o utilizador sobre a falha
                    _connectionStatus.value = ConnectionStatus.ERROR
                }
            }
        } else {
            // Log de falha de conexão
            android.util.Log.w("BT_DEBUG", "AVISO: Socket não conectado. O comando '$command' foi ignorado.")
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (e: Exception) { /* Ignore */ }
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        job.cancel()
    }

    fun requestScheduleList() {
        scheduleListBuilder.clear()
        sendCommand("L")
    }

    fun createOrUpdateSchedule(schedule: Schedule) {
        val command = "C,${schedule.index},${schedule.startHour},${schedule.startMinute},${schedule.endHour},${schedule.endMinute},${schedule.daysFlags}"
        sendCommand(command)
    }

    fun deleteSchedule(index: Int) {
        sendCommand("D,$index")
    }

    fun requestStatusOnce() {
        sendCommand("S")
    }

    fun requestTime() {
        sendCommand("T")
    }
    // Dentro da classe ArduinoBluetoothManager

    fun syncRtcWithPhoneTime() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH é baseado em zero (0-11)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val command = "U,$year,$month,$day,$hour,$minute,$second"
        sendCommand(command)
    }

    fun activeManualMode(){
        sendCommand("M")
    }
    fun disableManualMode(){
        sendCommand("A")
    }
}
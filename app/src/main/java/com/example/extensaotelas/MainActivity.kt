package com.example.extensaotelas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.extensaotelas.BancoDeDados.AppDatabase
import com.google.android.material.button.MaterialButton
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate // Lib do gráfico
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.Manifest
import android.content.pm.PackageManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID
import android.app.AlertDialog
import java.io.InputStream

class MainActivity : AppCompatActivity() {
	private var discoveredDevices = mutableListOf<BluetoothDevice>()
	private var discoveryDialog: AlertDialog? = null
	private lateinit var bluetoothAdapter: BluetoothAdapter
	private lateinit var textViewStatus: TextView
	private lateinit var btnGerenciarHorario: MaterialButton
	private var lastTemp: Float? = null
	private var lastAr: Float? = null
	private var lastSolo: Float? = null
	private var connectedThread: ConnectedThread? = null
	private var connectedSocket: BluetoothSocket? = null

	private val discoveryReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (intent?.action == BluetoothDevice.ACTION_FOUND) {
				val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
				device?.let {
					if (!discoveredDevices.any { d -> d.address == it.address }) {
						discoveredDevices.add(it)
						updateDiscoveryDialog()
					}
				}
			} else if (intent?.action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
				if (discoveredDevices.isEmpty()) {
					Toast.makeText(this@MainActivity, "Nenhum dispositivo encontrado", Toast.LENGTH_SHORT).show()
					textViewStatus.text = "Desconectado"
					textViewStatus.setTextColor(Color.RED)
					btnGerenciarHorario.isEnabled = false
					discoveryDialog?.dismiss()
				}
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_main)

		//instanciar o bd
		val db = Room.databaseBuilder(
			applicationContext,
			AppDatabase::class.java, "database-name"
		).build()

		btnGerenciarHorario = findViewById<MaterialButton>(R.id.btnGerenciarHorario)
		btnGerenciarHorario.isEnabled = false
		btnGerenciarHorario.setOnClickListener {
			val intent = Intent(this, ListaHorariosActivity::class.java)
			startActivity(intent)
		}

		val btnBluetooth = findViewById<MaterialButton>(R.id.btnBluetooth)
		textViewStatus = findViewById<TextView>(R.id.textView2)
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

		// Registrar receiver para discovery
		val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
		registerReceiver(discoveryReceiver, filter)

		btnBluetooth.setOnClickListener {
			if (bluetoothAdapter == null) {
				Toast.makeText(this, "Bluetooth não suportado neste dispositivo", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}
			// Solicitar permissões se necessário
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(
					this,
					arrayOf(
						Manifest.permission.BLUETOOTH_CONNECT,
						Manifest.permission.BLUETOOTH_SCAN,
						Manifest.permission.ACCESS_FINE_LOCATION
					),
					1001
				)
				return@setOnClickListener
			}
			if (!bluetoothAdapter.isEnabled) {
				Toast.makeText(this, "Ative o Bluetooth primeiro", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}
			// Procurar dispositivos pareados
			val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
			if (!pairedDevices.isNullOrEmpty()) {
				// Mostrar lista de dispositivos pareados para o usuário escolher
				val deviceList = pairedDevices.map { it.name + "\n" + it.address }
				val deviceArray = deviceList.toTypedArray()
				val devices = pairedDevices.toList()
				AlertDialog.Builder(this)
					.setTitle("Escolha um dispositivo pareado")
					.setItems(deviceArray) { _, which ->
						val device = devices[which]
						ConnectThread(device, textViewStatus).start()
					}
					.setNegativeButton("Cancelar", null)
					.show()
			} else {
				// Não há dispositivos pareados, iniciar discovery
				discoveredDevices.clear()
				startDiscoveryAndShowDialog()
			}
		}

	}

	private fun startDiscoveryAndShowDialog() {
		if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
		bluetoothAdapter.startDiscovery()
		val deviceNames = mutableListOf("Procurando dispositivos...")
		discoveryDialog = AlertDialog.Builder(this)
			.setTitle("Dispositivos próximos")
			.setSingleChoiceItems(deviceNames.toTypedArray(), -1, null)
			.setNegativeButton("Cancelar") { _, _ ->
				bluetoothAdapter.cancelDiscovery()
			}
			.setPositiveButton("Conectar") { dialog, _ ->
				val listView = (dialog as AlertDialog).listView
				val pos = listView.checkedItemPosition
				if (pos >= 0 && pos < discoveredDevices.size) {
					val device = discoveredDevices[pos]
					bluetoothAdapter.cancelDiscovery()
					ConnectThread(device, textViewStatus).start()
				} else {
					Toast.makeText(this, "Selecione um dispositivo", Toast.LENGTH_SHORT).show()
				}
			}
			.create()
		discoveryDialog?.show()
	}

	private fun updateDiscoveryDialog() {
		discoveryDialog?.let { dialog ->
			val names = discoveredDevices.map { (it.name ?: "(Sem nome)") + "\n" + it.address }
			val listView = dialog.listView
			val adapter = listView.adapter as? android.widget.ArrayAdapter<String>
			if (adapter != null) {
				adapter.clear()
				adapter.addAll(names)
				adapter.notifyDataSetChanged()
			} else {
				listView.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, names)
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		unregisterReceiver(discoveryReceiver)
		connectedThread?.cancel()
		connectedSocket?.let {
			try { it.close() } catch (_: IOException) {}
		}
		btnGerenciarHorario.isEnabled = false
	}

	// Thread para conectar ao dispositivo Bluetooth
	inner class ConnectThread(private val device: BluetoothDevice, private val statusView: TextView) : Thread() {
		private val uuid: UUID = device.uuids?.firstOrNull()?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
		override fun run() {
			var socket: BluetoothSocket? = null
			try {
				// Cancelar discovery para acelerar a conexão
				if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
				socket = device.createRfcommSocketToServiceRecord(uuid)
				socket.connect()
				connectedSocket = socket
				runOnUiThread {
					statusView.text = "Conectado a: ${device.name}"
					statusView.setTextColor(Color.GREEN)
					Toast.makeText(this@MainActivity, "Conectado a: ${device.name}", Toast.LENGTH_SHORT).show()
					btnGerenciarHorario.isEnabled = true
				}
				// Iniciar thread de leitura
				connectedThread?.cancel()
				connectedThread = ConnectedThread(socket.inputStream)
				connectedThread?.start()
			} catch (e: IOException) {
				runOnUiThread {
					statusView.text = "Falha na conexão"
					statusView.setTextColor(Color.RED)
					Toast.makeText(this@MainActivity, "Erro ao conectar: ${e.message}", Toast.LENGTH_SHORT).show()
					btnGerenciarHorario.isEnabled = false
				}
				try { socket?.close() } catch (_: IOException) {}
			}
		}
	}

	private inner class ConnectedThread(private val input: InputStream) : Thread() {
		@Volatile private var running = true
		override fun run() {
			val buffer = ByteArray(1024)
			var bytesRead: Int
			val sb = StringBuilder()
			while (running) {
				try {
					bytesRead = input.read(buffer)
					if (bytesRead == -1) break
					val chunk = String(buffer, 0, bytesRead, Charsets.UTF_8)
					sb.append(chunk)
					var newlineIndex = sb.indexOf("\n")
					while (newlineIndex >= 0) {
						val line = sb.substring(0, newlineIndex).trim()
						if (line.isNotEmpty()) {
							processReceivedData(line)
						}
						sb.delete(0, newlineIndex + 1)
						newlineIndex = sb.indexOf("\n")
					}
				} catch (e: IOException) {
					break
				}
			}
			// Encerrado: considerar desconectado
			runOnUiThread {
				textViewStatus.text = "Desconectado"
				textViewStatus.setTextColor(Color.RED)
				btnGerenciarHorario.isEnabled = false
			}
		}
		fun cancel() {
			running = false
			try { input.close() } catch (_: IOException) {}
		}
	}

	private fun processReceivedData(data: String) {
        // Exemplo de linha: T_amb: 23 °C\tUmidade do ar: 60 %
        // Ou: Umidade do Solo: 45 %

		// Referências aos novos TextViews
		val txtTemperaturaValor = findViewById<TextView>(R.id.txtTemperaturaValor)
		val txtUmidadeArValor = findViewById<TextView>(R.id.txtUmidadeArValor)
		val txtUmidadeSoloValor = findViewById<TextView>(R.id.txtUmidadeSoloValor)

        if (data.contains("T_amb:")) {
            val regex = Regex("T_amb:\\s*(\\d+)\\s*°C\\s*\\tUmidade do ar:\\s*(\\d+)\\s*%")
            val match = regex.find(data)
            if (match != null) {
                val temp = match.groupValues[1].toFloat()
                val ar = match.groupValues[2].toFloat()
                lastTemp = temp
                lastAr = ar
				runOnUiThread {
					txtTemperaturaValor.text = "$lastTemp °C"
					txtUmidadeArValor.text = "$lastAr%"

				}
            }
        } else if (data.contains("Umidade do Solo:")) {
            val regex = Regex("Umidade do Solo:\\s*(\\d+)\\s*%")
            val match = regex.find(data)
            if (match != null) {
                val solo = match.groupValues[1].toFloat()
                lastSolo = solo
                runOnUiThread {
                    txtUmidadeSoloValor.text = "$lastSolo%"
                }
            }
        }
    }
}
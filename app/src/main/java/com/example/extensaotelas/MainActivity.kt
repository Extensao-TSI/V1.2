package com.example.extensaotelas

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.extensaotelas.BancoDeDados.AppDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.io.InputStream
import java.util.UUID

// Enum para gerenciar o estado da conexão de forma explícita e centralizada
private enum class ConnectionState {
	DISCONNECTED,
	CONNECTING,
	CONNECTED
}

@SuppressLint("MissingPermission") // As permissões são checadas em tempo de execução
class MainActivity : AppCompatActivity() {

	private var discoveredDevices = mutableListOf<BluetoothDevice>()
	private var discoveryDialog: AlertDialog? = null
	private lateinit var bluetoothAdapter: BluetoothAdapter
	private lateinit var textViewStatus: TextView
	private lateinit var btnGerenciarHorario: MaterialButton

	private var lastTemp: Float? = null
	private var lastAr: Float? = null
	private var lastSolo: Float? = null

	private var connectThread: ConnectThread? = null
	private var connectedThread: ConnectedThread? = null

	private val discoveryReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			val action: String? = intent?.action
			when (action) {
				BluetoothDevice.ACTION_FOUND -> {
					val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
					device?.let {
						if (it.name != null && !discoveredDevices.any { d -> d.address == it.address }) {
							discoveredDevices.add(it)
							updateDiscoveryDialog()
						}
					}
				}
				BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
					if (discoveredDevices.isEmpty() && discoveryDialog?.isShowing == true) {
						Toast.makeText(this@MainActivity, "Nenhum dispositivo encontrado", Toast.LENGTH_SHORT).show()
						updateUI(ConnectionState.DISCONNECTED)
						discoveryDialog?.dismiss()
					}
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

		btnGerenciarHorario = findViewById(R.id.btnGerenciarHorario)
		val btnBluetooth = findViewById<MaterialButton>(R.id.btnBluetooth)
		textViewStatus = findViewById(R.id.textView2)

		updateUI(ConnectionState.DISCONNECTED) // Define o estado inicial da UI

		btnGerenciarHorario.setOnClickListener {
			if (BluetoothConnectionManager.isConnected.value == true) {
				// Se estiver conectado, abre a ListaHorariosActivity normalmente.
				val intent = Intent(this, ListaHorariosActivity::class.java)
				startActivity(intent)
			} else {
				val rootView = findViewById<android.view.View>(android.R.id.content)

				val snackbar = Snackbar.make(
					rootView,
					"É necessário estar conectado ao Bluetooth.",
					Snackbar.LENGTH_INDEFINITE)


				snackbar.setAction("OK") {

					snackbar.dismiss()
				}

				snackbar.show()
			}
		}

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

		val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
		registerReceiver(discoveryReceiver, filter)

		btnBluetooth.setOnClickListener {
			if (bluetoothAdapter == null) {
				Toast.makeText(this, "Bluetooth não suportado", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}
			if (checkPermissions()) {
				showDeviceList()
			} else {
				requestPermissions()
			}
		}
	}

	private fun checkPermissions(): Boolean {
		return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
				ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
				ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
	}

	private fun requestPermissions() {
		ActivityCompat.requestPermissions(
			this,
			arrayOf(
				Manifest.permission.BLUETOOTH_CONNECT,
				Manifest.permission.BLUETOOTH_SCAN,
				Manifest.permission.ACCESS_FINE_LOCATION
			),
			1001
		)
	}

	private fun showDeviceList() {
		if (!bluetoothAdapter.isEnabled) {
			Toast.makeText(this, "Ative o Bluetooth primeiro", Toast.LENGTH_SHORT).show()
			return
		}

		val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
		if (!pairedDevices.isNullOrEmpty()) {
			val deviceList = pairedDevices.map { it.name + "\n" + it.address }
			AlertDialog.Builder(this)
				.setTitle("Escolha um dispositivo pareado")
				.setItems(deviceList.toTypedArray()) { _, which ->
					val device = pairedDevices.elementAt(which)
					connectToDevice(device)
				}
				.setNegativeButton("Procurar novos") { _, _ ->
					startDiscoveryAndShowDialog()
				}
				.show()
		} else {
			startDiscoveryAndShowDialog()
		}
	}

	private fun startDiscoveryAndShowDialog() {
		if (bluetoothAdapter.isDiscovering) {
			bluetoothAdapter.cancelDiscovery()
		}
		discoveredDevices.clear()
		bluetoothAdapter.startDiscovery()
		discoveryDialog = AlertDialog.Builder(this)
			.setTitle("Dispositivos próximos")
			.setSingleChoiceItems(arrayOf("Procurando..."), -1, null)
			.setNegativeButton("Cancelar") { _, _ ->
				bluetoothAdapter.cancelDiscovery()
			}
			.setPositiveButton("Conectar") { dialog, _ ->
				val listView = (dialog as AlertDialog).listView
				val pos = listView.checkedItemPosition
				if (pos >= 0 && pos < discoveredDevices.size) {
					val device = discoveredDevices[pos]
					bluetoothAdapter.cancelDiscovery()
					connectToDevice(device)
				} else {
					Toast.makeText(this, "Selecione um dispositivo", Toast.LENGTH_SHORT).show()
				}
			}
			.create()
		discoveryDialog?.show()
	}

	private fun updateDiscoveryDialog() {
		discoveryDialog?.let { dialog ->
			val names = discoveredDevices.map { (it.name ?: "Desconhecido") + "\n" + it.address }
			val listView = dialog.listView
			listView.adapter = android.widget.ArrayAdapter(
				this,
				android.R.layout.simple_list_item_single_choice,
				names
			)
		}
	}

	// Função centralizada para iniciar a conexão
	private fun connectToDevice(device: BluetoothDevice) {
		connectThread?.cancel() // Cancela qualquer tentativa anterior
		connectThread = ConnectThread(device)
		connectThread?.start()
	}

	// Função centralizada para atualizar a UI com base no estado da conexão
	private fun updateUI(state: ConnectionState, deviceName: String? = null) {
		runOnUiThread {
			when (state) {
				ConnectionState.CONNECTING -> {
					textViewStatus.text = "Conectando..."
					textViewStatus.setTextColor(Color.DKGRAY)
				}
				ConnectionState.CONNECTED -> {
					textViewStatus.text = "Conectado a: $deviceName"
					textViewStatus.setTextColor(Color.GREEN)
				}
				ConnectionState.DISCONNECTED -> {
					textViewStatus.text = "Desconectado"
					textViewStatus.setTextColor(Color.RED)
				}
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		Log.d("BT_DEBUG", "onDestroy chamado. Atualizando estado para DESCONECTADO.")
		BluetoothConnectionManager.setConnectionStatus(false)
		unregisterReceiver(discoveryReceiver)
		connectThread?.cancel()
		connectedThread?.cancel()
	}

	// --- THREAD DE CONEXÃO REFAtoRADA ---
	private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
		private var mmSocket: BluetoothSocket? = null

		override fun run() {
			updateUI(ConnectionState.CONNECTING)
			if (bluetoothAdapter.isDiscovering) {
				bluetoothAdapter.cancelDiscovery()
			}

			// Tenta obter o socket de forma segura e, se falhar, de forma insegura
			val uuid: UUID = device.uuids?.firstOrNull()?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
			mmSocket = try {
				device.createRfcommSocketToServiceRecord(uuid)
			} catch (e: IOException) {
				Log.w("BT_CONNECT", "Falha no socket seguro, tentando inseguro... ${e.message}")
				try {
					device.createInsecureRfcommSocketToServiceRecord(uuid)
				} catch (e2: IOException) {
					Log.e("BT_CONNECT", "Falha ao obter socket: ${e2.message}")
					null
				}
			}
			if (mmSocket == null) {
				Log.e("BT_CONNECT", "Falha final ao criar o socket. Abortando a conexão.")
				updateUI(ConnectionState.DISCONNECTED)
				runOnUiThread { Toast.makeText(this@MainActivity, "Erro ao iniciar conexão.", Toast.LENGTH_SHORT).show() }
				return // Sai da thread para evitar o crash.
			}

			// Tenta se conectar
			try {
				mmSocket!!.connect()
				Log.d("BT_CONNECT", "Conexão bem-sucedida!")
				manageConnectedSocket(mmSocket!!)
			} catch (e: IOException) {
				Log.e("BT_CONNECT", "Falha na conexão: ${e.message}")
				updateUI(ConnectionState.DISCONNECTED)
				runOnUiThread { Toast.makeText(this@MainActivity, "Não foi possível conectar.", Toast.LENGTH_SHORT).show() }
				cancel()
			}
		}

		fun cancel() {
			try {
				mmSocket?.close()
			} catch (e: IOException) {
				Log.e("BT_CONNECT", "Não foi possível fechar o socket", e)
			}
		}
	}

	private fun manageConnectedSocket(socket: BluetoothSocket) {
		BluetoothConnectionManager.setConnectionStatus(true)
		updateUI(ConnectionState.CONNECTED, socket.remoteDevice.name)
		connectedThread?.cancel()
		connectedThread = ConnectedThread(socket)
		connectedThread?.start()
	}

	// --- THREAD DE LEITURA DE DADOS REFAtoRADA ---
	private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
		private val inputStream: InputStream = socket.inputStream

		override fun run() {
			val buffer = ByteArray(1024)
			var numBytes: Int
			val stringBuilder = StringBuilder()

			while (true) {
				try {
					numBytes = inputStream.read(buffer)
					if (numBytes == -1) break // Conexão fechada

					val readMessage = String(buffer, 0, numBytes)
					stringBuilder.append(readMessage)

					var endOfLineIndex = indexOfEndOfLine(stringBuilder)
					while (endOfLineIndex >= 0) {
						val completeLine = stringBuilder.substring(0, endOfLineIndex).trim()
						stringBuilder.delete(0, endOfLineIndex + 1)
						if (completeLine.isNotEmpty()) {
							Log.d("BT_RX", "Linha: $completeLine")
							runOnUiThread { processReceivedData(completeLine) }
						}
						endOfLineIndex = indexOfEndOfLine(stringBuilder)
					}
				} catch (e: IOException) {
					Log.d("BT_RX", "Conexão perdida: ${e.message}")
					break
				}
			}
			BluetoothConnectionManager.setConnectionStatus(false)
			updateUI(ConnectionState.DISCONNECTED)
		}

		fun cancel() {
			try {
				socket.close()
			} catch (e: IOException) {
				Log.e("ConnectedThread", "Falha ao fechar socket", e)
			}
		}
	}

	// --- SUAS FUNÇÕES DE PROCESSAMENTO DE DADOS (sem alterações) ---
	private fun indexOfEndOfLine(sb: StringBuilder): Int {
		val nIdx = sb.indexOf('\n')
		val rIdx = sb.indexOf('\r')
		return when {
			nIdx == -1 && rIdx == -1 -> -1
			nIdx == -1 -> rIdx
			rIdx == -1 -> nIdx
			else -> minOf(nIdx, rIdx)
		}
	}

	private fun processReceivedData(data: String) {
		val txtTemperaturaValor = findViewById<TextView>(R.id.txtTemperaturaValor)
		val txtUmidadeArValor = findViewById<TextView>(R.id.txtUmidadeArValor)
		val txtUmidadeSoloValor = findViewById<TextView>(R.id.txtUmidadeSoloValor)
		val s = data.replace(',', '.')
		Log.d("BT_RX", "Processando: $s")
		var updated = false
		run {
			val rx = Regex("(T(?:emp)?)[^0-9-]*(-?[0-9]+(?:\\.[0-9]+)?)|Umidade\\s*do\\s*ar[^0-9-]*(-?[0-9]+(?:\\.[0-9]+)?)|H(?:um)?[^0-9-]*(-?[0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE)
			val matches = rx.findAll(s).toList()
			var t: Float? = null
			var h: Float? = null
			for (m in matches) {
				val full = m.value.lowercase()
				val numStr = Regex("-?[0-9]+(?:\\.[0-9]+)?").find(full)?.value
				val v = numStr?.toFloatOrNull() ?: continue
				if (full.contains("t")) t = v else h = v
			}
			if (t != null || h != null) {
				if (t != null) lastTemp = t
				if (h != null) lastAr = h
				lastTemp?.let { txtTemperaturaValor.text = String.format("%.0f °C", it) }
				lastAr?.let { txtUmidadeArValor.text = String.format("%.0f%%", it) }
				updated = true
			}
		}
		if (!updated) {
			val rxSolo = Regex("(solo|soil|s)[^0-9-]*(-?[0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE)
			val m = rxSolo.find(s)
			if (m != null) {
				val v = m.groupValues.last().toFloatOrNull()
				if (v != null) {
					lastSolo = v
					txtUmidadeSoloValor.text = String.format("%.0f%%", v)
					updated = true
				}
			}
		}
		if (!updated && (s.contains(";") || s.contains(",") || s.contains(" "))) {
			val tokens = s.split(';', ',', ' ', '\t').map { it.trim() }.filter { it.isNotEmpty() }
			if (tokens.size >= 2) {
				val t = tokens.getOrNull(0)?.toFloatOrNull()
				val h = tokens.getOrNull(1)?.toFloatOrNull()
				val so = tokens.getOrNull(2)?.toFloatOrNull()
				if (t != null) { lastTemp = t; txtTemperaturaValor.text = String.format("%.1f °C", t) }
				if (h != null) { lastAr = h; txtUmidadeArValor.text = String.format("%.0f%%", h) }
				if (so != null) { lastSolo = so; txtUmidadeSoloValor.text = String.format("%.0f%%", so) }
			}
		}
	}
}
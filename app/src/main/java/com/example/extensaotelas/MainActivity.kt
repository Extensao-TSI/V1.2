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
import android.util.Log

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
				// Tentativa 1: socket seguro padrão
				socket = device.createRfcommSocketToServiceRecord(uuid)
				socket.connect()
			} catch (e1: IOException) {
				Log.w("BT_CONNECT", "Secure connect falhou: ${e1.message}. Tentando insecure...")
				try {
					// Tentativa 2: socket inseguro (HC-05 frequentemente precisa)
					socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
					socket.connect()
				} catch (e2: IOException) {
					Log.e("BT_CONNECT", "Insecure connect falhou: ${e2.message}")
					runOnUiThread {
						statusView.text = "Falha na conexão"
						statusView.setTextColor(Color.RED)
						Toast.makeText(this@MainActivity, "Erro ao conectar: ${e2.message}", Toast.LENGTH_SHORT).show()
						btnGerenciarHorario.isEnabled = false
					}
					try { socket?.close() } catch (_: IOException) {}
					return
				}
			}

			// Conectado
			connectedSocket = socket
			runOnUiThread {
				statusView.text = "Conectado a: ${device.name}"
				statusView.setTextColor(Color.GREEN)
				Toast.makeText(this@MainActivity, "Conectado a: ${device.name}", Toast.LENGTH_SHORT).show()
				btnGerenciarHorario.isEnabled = true
			}
			// Iniciar thread de leitura
			connectedThread?.cancel()
			connectedThread = ConnectedThread(socket!!.inputStream)
			connectedThread?.start()
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
					var idxNewLine = indexOfEndOfLine(sb)
					while (idxNewLine >= 0) {
						val line = sb.substring(0, idxNewLine).trim()
						if (line.isNotEmpty()) {
							Log.d("BT_RX", "Linha recebida: $line")
							processReceivedData(line)
						}
						sb.delete(0, idxNewLine + 1)
						idxNewLine = indexOfEndOfLine(sb)
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

	private fun indexOfEndOfLine(sb: StringBuilder): Int {
		val nIdx = sb.indexOf("\n")
		val rIdx = sb.indexOf("\r")
		return when {
			nIdx == -1 && rIdx == -1 -> -1
			nIdx == -1 -> rIdx
			rIdx == -1 -> nIdx
			else -> minOf(nIdx, rIdx)
		}
	}

	private fun processReceivedData(data: String) {
        // Suporta vários formatos: com rótulos (T/Temp, H/Umidade, S/Solo), CSV, etc.

		val txtTemperaturaValor = findViewById<TextView>(R.id.txtTemperaturaValor)
		val txtUmidadeArValor = findViewById<TextView>(R.id.txtUmidadeArValor)
		val txtUmidadeSoloValor = findViewById<TextView>(R.id.txtUmidadeSoloValor)

		val s = data.replace(',', '.')
		Log.d("BT_RX", "Processando: $s")

		var updated = false

		// 1) Linha com temperatura e umidade do ar
		run {
			val rx = Regex("(T(?:emp)?)[^0-9-]*(-?[0-9]+(?:\\.[0-9]+)?)|Umidade\\s*do\\s*ar[^0-9-]*(-?[0-9]+(?:\\.[0-9]+)?)|H(?:um)?[^0-9-]*(-?[0-9]+(?:\\.[0-9]+)?)",
				RegexOption.IGNORE_CASE)
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
				runOnUiThread {
					lastTemp?.let { txtTemperaturaValor.text = String.format("%.0f °C", it) }
					lastAr?.let { txtUmidadeArValor.text = String.format("%.0f%%", it) }
				}
				updated = true
				Log.d("BT_RX", "Atualizado: Temp=$lastTemp, Ar=$lastAr")
			}
		}

		// 2) Linha com umidade do solo
		if (!updated) {
			val rxSolo = Regex("(solo|soil|s)[^0-9-]*(-?[0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE)
			val m = rxSolo.find(s)
			if (m != null) {
				val v = m.groupValues.last().toFloatOrNull()
				if (v != null) {
					lastSolo = v
					runOnUiThread { txtUmidadeSoloValor.text = String.format("%.0f%%", v) }
					updated = true
					Log.d("BT_RX", "Atualizado: Solo=$lastSolo")
				}
			}
		}

		// 3) CSV: temp,ar,solo
		if (!updated && s.contains(";", true) || s.contains(",") || s.contains(" ")) {
			val tokens = s.split(';', ',', ' ', '\t').map { it.trim() }.filter { it.isNotEmpty() }
			if (tokens.size >= 2) {
				val t = tokens.getOrNull(0)?.toFloatOrNull()
				val h = tokens.getOrNull(1)?.toFloatOrNull()
				val so = tokens.getOrNull(2)?.toFloatOrNull()
				if (t != null) { lastTemp = t; runOnUiThread { txtTemperaturaValor.text = String.format("%.1f °C", t) } }
				if (h != null) { lastAr = h; runOnUiThread { txtUmidadeArValor.text = String.format("%.0f%%", h) } }
				if (so != null) { lastSolo = so; runOnUiThread { txtUmidadeSoloValor.text = String.format("%.0f%%", so) } }
				if (t != null || h != null || so != null) {
					updated = true
					Log.d("BT_RX", "Atualizado CSV: Temp=$lastTemp, Ar=$lastAr, Solo=$lastSolo")
				}
			}
		}
	}
}
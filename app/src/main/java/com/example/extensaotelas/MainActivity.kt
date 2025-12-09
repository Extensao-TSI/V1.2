package com.example.extensaotelas

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class MainActivity : ComponentActivity() {

	private val viewModel: MainViewModel by viewModels()
	private lateinit var bluetoothAdapter: BluetoothAdapter
	private lateinit var textViewStatus: TextView
	private lateinit var txtTemperaturaValor: TextView
	private lateinit var txtUmidadeArValor: TextView
	private lateinit var txtUmidadeSoloValor: TextView

	private var statusPollJob: Job? = null
	private lateinit var btnLigaDesliga: MaterialButton


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		textViewStatus = findViewById(R.id.textView2)
		txtTemperaturaValor = findViewById(R.id.txtTemperaturaValor)
		txtUmidadeArValor = findViewById(R.id.txtUmidadeArValor)
		txtUmidadeSoloValor = findViewById(R.id.txtUmidadeSoloValor)


		setupUI()
		observeViewModel()

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
	}

	private fun setupUI() {
		findViewById<MaterialButton>(R.id.btnGerenciarHorario).setOnClickListener {
			if (viewModel.connectionStatus.value == ConnectionStatus.CONNECTED) {
				startActivity(Intent(this, ListaHorariosActivity::class.java))
			} else {
				Toast.makeText(this, "Você precisa estar conectado para gerenciar horários", Toast.LENGTH_SHORT).show()
			}
		}

		findViewById<MaterialButton>(R.id.btnBluetooth).setOnClickListener {
			handleBluetoothConnection()
		}
		val tvSobre = findViewById<TextView>(R.id.textView3)
		tvSobre.setOnClickListener {
			AlertDialog.Builder(this)
				.setTitle("Sobre o Projeto")
				.setMessage(
					"Coordenador do Projeto: Leonardo Lachi Manetti\n" +
					"Professores Orientadores: Jonathas Leontino Medina, Eder de Souza Rodrigues\n" +
					"Nomes dos Estudantes: João Brito Fachineli Brito, "
				)
				.setPositiveButton("OK", null)
				.show()
		}
	}

	// Dentro de MainActivity.kt

	// Dentro de MainActivity.kt

	private fun observeViewModel() {
		lifecycleScope.launch {
			viewModel.connectionStatus.collectLatest { status ->
				when(status) {
					ConnectionStatus.CONNECTED -> {
						textViewStatus.text = "Conectado"
						textViewStatus.setTextColor(Color.GREEN)
						Toast.makeText(this@MainActivity, "Conectado. Sincronizando horário...", Toast.LENGTH_SHORT).show()
						viewModel.syncRtc()

						// --- INÍCIO DA CORREÇÃO ---
						// REMOVEMOS COMPLETAMENTE O statusPollJob DAQUI
						// O Arduino já envia os dados automaticamente, não precisamos de os pedir.
						statusPollJob?.cancel()
						// --- FIM DA CORREÇÃO ---
					}
					ConnectionStatus.DISCONNECTED -> {
						textViewStatus.text = "Desconectado"
						textViewStatus.setTextColor(Color.RED)
						statusPollJob?.cancel()
					}
					ConnectionStatus.CONNECTING -> {
						textViewStatus.text = "Conectando..."
						textViewStatus.setTextColor(Color.BLUE)
					}
					ConnectionStatus.ERROR -> {
						textViewStatus.text = "Erro na conexão"
						textViewStatus.setTextColor(Color.RED)
					}
				}
			}
		}

		lifecycleScope.launch {
			viewModel.sensorData.collectLatest { data ->
				data?.let {
					txtTemperaturaValor.text = String.format("%.1f °C", it.temperature)
					txtUmidadeArValor.text = String.format("%.0f%%", it.airHumidity)
					txtUmidadeSoloValor.text = String.format("%d%%", it.soilHumidity)
				}
			}
		}
	}

	private fun handleBluetoothConnection() {
		if (checkPermissions()) {
			if (!bluetoothAdapter.isEnabled) {
				Toast.makeText(this, "Por favor, ative o Bluetooth.", Toast.LENGTH_SHORT).show()
				return
			}
			val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
			if (pairedDevices.isNullOrEmpty()) {
				Toast.makeText(this, "Nenhum dispositivo pareado encontrado.", Toast.LENGTH_SHORT).show()
				return
			}

			val deviceList = pairedDevices.map { it.name to it }.toTypedArray()
			AlertDialog.Builder(this)
				.setTitle("Escolha um dispositivo")
				.setItems(deviceList.map { it.first }.toTypedArray()) { _, which ->
					val device = deviceList[which].second
					viewModel.connectToDevice(device)
				}
				.show()
		}
	}

	private fun checkPermissions(): Boolean {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1001)
			return false
		}
		return true
	}

	override fun onDestroy() {
		super.onDestroy()
		viewModel.disconnect()
	}
}
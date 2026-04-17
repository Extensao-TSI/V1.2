package com.example.extensaotelas

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
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
	private var bluetoothAdapter: BluetoothAdapter? = null
	private lateinit var textViewStatus: TextView
	private lateinit var txtTemperaturaValor: TextView
	private lateinit var txtUmidadeArValor: TextView
	private lateinit var txtUmidadeSoloValor: TextView

	private var statusPollJob: Job? = null
	private lateinit var btnLigaDesliga: MaterialButton
	private lateinit var btnBluetooth: MaterialButton

	private val requestPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { permissions ->
		if (permissions.entries.all { it.value }) {
			iniciarConexaoBluetooth()
		} else {
			Toast.makeText(this, "Permissões necessárias não concedidas", Toast.LENGTH_SHORT).show()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		textViewStatus = findViewById(R.id.textView2)
		txtTemperaturaValor = findViewById(R.id.txtTemperaturaValor)
		txtUmidadeArValor = findViewById(R.id.txtUmidadeArValor)
		txtUmidadeSoloValor = findViewById(R.id.txtUmidadeSoloValor)
		btnBluetooth = findViewById(R.id.btnBluetooth)


		setupUI()
		observeViewModel()

		val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		bluetoothAdapter = bluetoothManager.adapter
	}

	private fun setupUI() {
		findViewById<MaterialButton>(R.id.btnGerenciarHorario).setOnClickListener {
			if (viewModel.connectionStatus.value == ConnectionStatus.CONNECTED) {
				startActivity(Intent(this, ListaHorariosActivity::class.java))
			} else {
				Toast.makeText(this, "Você precisa estar conectado para gerenciar horários", Toast.LENGTH_SHORT).show()
			}
		}

		btnBluetooth.setOnClickListener {
			if (viewModel.connectionStatus.value == ConnectionStatus.CONNECTED) {
				viewModel.disconnect()
			} else {
				handleBluetoothConnection()
			}
		}
		val tvSobre = findViewById<Button>(R.id.btnSobre)
		tvSobre.setOnClickListener {
			AlertDialog.Builder(this)
				.setTitle("Sobre o Projeto")
				.setMessage(
					"Coordenador do Projeto: Leonardo Lachi Manetti\n" +
					"Professores Orientadores: Jonathas Leontino Medina, Eder de Souza Rodrigues\n" +
					"Nomes dos Estudantes: " +
                            "Gabriel Hideki Maekawa\n" +
                            "Luís César Ramires Bezerra \n" +
                            "Fillipe Coppes Furtado\n" +
                            "Isaque Melo de Paula \n" +
                            "João Pedro Fachineli Brito\n" +
                            "Pedro Henrique Pereira de Matos \n" +
                            "Marcos da Rosa Sotomaior \n" +
                            "Vitor Hugo Ferreira Menoni\n"

				)
				.setPositiveButton("OK", null)
				.show()
		}
	}


	private fun observeViewModel() {
		lifecycleScope.launch {
			viewModel.connectionStatus.collectLatest { status ->
				when(status) {
					ConnectionStatus.CONNECTED -> {
						textViewStatus.text = "Conectado"
						textViewStatus.setTextColor(Color.GREEN)
						btnBluetooth.text = "DESCONECTAR"
						Toast.makeText(this@MainActivity, "Conectado. Sincronizando horário...", Toast.LENGTH_SHORT).show()
						viewModel.syncRtc()

						// O Arduino já envia os dados automaticamente, não precisamos de os pedir.
						statusPollJob?.cancel()
					}
					ConnectionStatus.DISCONNECTED -> {
						textViewStatus.text = "Desconectado"
						textViewStatus.setTextColor(Color.RED)
						btnBluetooth.text = "CONECTAR DISPOSITIVOS"
						statusPollJob?.cancel()
					}
					ConnectionStatus.CONNECTING -> {
						textViewStatus.text = "Conectando..."
						textViewStatus.setTextColor(Color.BLUE)
						btnBluetooth.text = "CONECTANDO..."
					}
					ConnectionStatus.ERROR -> {
						textViewStatus.text = "Erro na conexão"
						textViewStatus.setTextColor(Color.RED)
						btnBluetooth.text = "CONECTAR DISPOSITIVOS"
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
		if (bluetoothAdapter == null) {
			Toast.makeText(this, "Este dispositivo não suporta Bluetooth.", Toast.LENGTH_SHORT).show()
			return
		}

		if (!checkPermissions()) {
			return
		}

		iniciarConexaoBluetooth()
	}

	private fun iniciarConexaoBluetooth() {
		val adapter = bluetoothAdapter ?: return

		if (!adapter.isEnabled) {
			Toast.makeText(this, "Por favor, ative o Bluetooth.", Toast.LENGTH_SHORT).show()
			return
		}

		// Cancelar discovery antes de listar/conectar — libera recursos da antena BT
		adapter.cancelDiscovery()

		val pairedDevices: Set<BluetoothDevice>? = adapter.bondedDevices
		if (pairedDevices.isNullOrEmpty()) {
			Toast.makeText(this, "Nenhum dispositivo pareado encontrado.", Toast.LENGTH_SHORT).show()
			return
		}

		val deviceList = pairedDevices.map {
			(it.name ?: "(Sem nome)") + "\n" + it.address to it
		}.toTypedArray()

		AlertDialog.Builder(this)
			.setTitle("Escolha um dispositivo")
			.setItems(deviceList.map { it.first }.toTypedArray()) { _, which ->
				val device = deviceList[which].second
				// Garantir que o discovery está cancelado antes de conectar
				adapter.cancelDiscovery()
				viewModel.connectToDevice(device)
			}
			.show()
	}

	private fun checkPermissions(): Boolean {
		val requiredPermissions = mutableListOf(Manifest.permission.BLUETOOTH_CONNECT)

		// BLUETOOTH_SCAN é necessário se quisermos fazer discovery no futuro
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
			requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
		}

		if (requiredPermissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
			requestPermissionLauncher.launch(requiredPermissions.toTypedArray())
			return false
		}
		return true
	}

	override fun onDestroy() {
		super.onDestroy()
		viewModel.disconnect()
	}
}
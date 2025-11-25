package com.example.extensaotelas

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class ListaHorariosActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HorarioAdapter
    private var scheduleList = mutableListOf<Schedule>()
    private lateinit var btnLigaDesliga: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_horarios)

        setupRecyclerView()

        findViewById<Button>(R.id.btnAdicionarHorario).setOnClickListener {
            startActivity(android.content.Intent(this, AdicionarHorarioActivity::class.java))
        }
        btnLigaDesliga = findViewById<MaterialButton>(R.id.btnLigarDesligar)
        findViewById<MaterialButton>(R.id.btnBluetooth).setOnClickListener {
            handleBluetoothConnection()
            if (viewModel.connectionStatus.value == ConnectionStatus.CONNECTED) {
                // Verifica o texto atual para saber qual ação tomar
                val isCurrentlyOn = btnLigaDesliga.text.toString().equals("DESLIGAR", ignoreCase = true)

                if (isCurrentlyOn) {
                    // Se o texto é "DESLIGAR", significa que o modo manual está ativo. Vamos desativá-lo.
                    viewModel.manualMode("A") // Envia comando para desativar (Modo Automático)
                    btnLigaDesliga.text = "LIGAR"
                    // Você pode querer aplicar um estilo diferente aqui se necessário
                    Toast.makeText(this, "Modo manual desativado.", Toast.LENGTH_SHORT).show()
                } else {
                    // Se o texto é "LIGAR", significa que o modo manual está inativo. Vamos ativá-lo.
                    viewModel.manualMode("M") // Envia comando para ativar (Modo Manual)
                    btnLigaDesliga.text = "DESLIGAR"
                    // Você pode querer aplicar um estilo diferente aqui se necessário
                    Toast.makeText(this, "Modo manual ativado.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Caso não haja conexão Bluetooth
                Toast.makeText(this, "Conecte-se ao Bluetooth para usar o modo manual.", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<MaterialButton>(R.id.btnLigarDesligar).setOnClickListener {
            if (viewModel.connectionStatus.value == ConnectionStatus.CONNECTED) {
                btnLigaDesliga.text = "DESLIGAR"
                btnLigaDesliga.setTextAppearance(R.style.AppButtonDesligar)

            } else {

                Toast.makeText(this, "Você precisa estar conectado para Ligar/Desligar", Toast.LENGTH_SHORT).show()
            }
        }

        observeSchedules()
    }

    override fun onResume() {
        super.onResume()
        // Pede a lista de horários ao Arduino sempre que a tela se torna visível
        viewModel.fetchSchedules()
        }

    override fun onPause() {
        super.onPause()

    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewHorarios)
        adapter = HorarioAdapter(
            scheduleList,
            onEditar = { schedule ->
                val intent = android.content.Intent(this, EditarHorarioActivity::class.java)
                intent.putExtra("index", schedule.index)
                intent.putExtra("startHour", schedule.startHour)
                intent.putExtra("startMinute", schedule.startMinute)
                intent.putExtra("endHour", schedule.endHour)
                intent.putExtra("endMinute", schedule.endMinute)
                intent.putExtra("daysFlags", schedule.daysFlags)
                startActivity(intent)
            },
            onExcluir = { schedule ->
                viewModel.deleteSchedule(schedule.index)
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun observeSchedules() {
        lifecycleScope.launch {
            viewModel.schedules.collectLatest { schedules ->
                scheduleList.clear()
                scheduleList.addAll(schedules)
                adapter.notifyDataSetChanged()
            }
        }
    }
}
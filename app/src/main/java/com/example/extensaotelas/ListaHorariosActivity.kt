package com.example.extensaotelas

import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_horarios)

        setupRecyclerView()

        findViewById<Button>(R.id.btnAdicionarHorario).setOnClickListener {
            startActivity(android.content.Intent(this, AdicionarHorarioActivity::class.java))
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
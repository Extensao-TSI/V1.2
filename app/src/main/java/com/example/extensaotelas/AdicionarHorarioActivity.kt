package com.example.extensaotelas

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class AdicionarHorarioActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var horaInicial: Int = 0
    private var minutosInicial: Int = 0
    private var horaFinal: Int = 0
    private var minutosFinal: Int = 0

    // Simplificação: usuário define índice e flags por agora via constantes ou futuras UI
    private var indice: Int = 0
    private var diasFlags: Int = 0

    private lateinit var cbDom: CheckBox
    private lateinit var cbSeg: CheckBox
    private lateinit var cbTer: CheckBox
    private lateinit var cbQua: CheckBox
    private lateinit var cbQui: CheckBox
    private lateinit var cbSex: CheckBox
    private lateinit var cbSab: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adicionar_horario)

        val btnHorarioInicial = findViewById<Button>(R.id.btnHorarioInicial)
        val btnHorarioFinal = findViewById<Button>(R.id.btnHorarioFinal)
        val btnSalvarHorario = findViewById<Button>(R.id.btnSalvarHorario)

        cbDom = findViewById(R.id.cbDom)
        cbSeg = findViewById(R.id.cbSeg)
        cbTer = findViewById(R.id.cbTer)
        cbQua = findViewById(R.id.cbQua)
        cbQui = findViewById(R.id.cbQui)
        cbSex = findViewById(R.id.cbSex)
        cbSab = findViewById(R.id.cbSab)

        btnHorarioInicial.setOnClickListener {
            mostrarTimePickerHorarioInicial(btnHorarioInicial)
        }

        btnHorarioFinal.setOnClickListener {
            mostrarTimePickerHorarioFinal(btnHorarioFinal)
        }

        btnSalvarHorario.setOnClickListener {
            // Usamos uma corrotina para lidar com a lógica de espera da reconexão
            lifecycleScope.launch {
                // Mostra um feedback visual de que algo está a acontecer
                val progressDialog = android.app.ProgressDialog.show(this@AdicionarHorarioActivity, "Aguarde", "A verificar conexão...", true)

                try {
                    // Se não estiver conectado, tenta reconectar
                    if (viewModel.connectionStatus.value != ConnectionStatus.CONNECTED) {
                        progressDialog.setMessage("Conexão perdida. A reconectar...")
                        viewModel.reconnect()

                        // Espera até 5 segundos para a conexão ser restabelecida
                        kotlinx.coroutines.withTimeoutOrNull(5000) {
                            viewModel.connectionStatus.filter { status: ConnectionStatus -> status == ConnectionStatus.CONNECTED }
                        }
                    }

                    // Após a tentativa de reconexão, verifica novamente
                    if (viewModel.connectionStatus.value != ConnectionStatus.CONNECTED) {
                        Toast.makeText(this@AdicionarHorarioActivity, "Falha ao reconectar. Por favor, volte à tela principal e conecte-se manualmente.", Toast.LENGTH_LONG).show()
                        return@launch // Sai da corrotina
                    }

                    // Se chegámos aqui, estamos conectados. Procede com a lógica de salvar.
                    progressDialog.setMessage("A enviar agendamento...")

                    // Validações (já as tinha, mantemos)
                    if (horaInicial !in 0..23 || horaFinal !in 0..23) {
                        Toast.makeText(this@AdicionarHorarioActivity, "Horário inválido", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    diasFlags = 0
                    if (cbDom.isChecked) diasFlags = diasFlags or 1
                    if (cbSeg.isChecked) diasFlags = diasFlags or 2
                    if (cbTer.isChecked) diasFlags = diasFlags or 4
                    if (cbQua.isChecked) diasFlags = diasFlags or 8
                    if (cbQui.isChecked) diasFlags = diasFlags or 16
                    if (cbSex.isChecked) diasFlags = diasFlags or 32
                    if (cbSab.isChecked) diasFlags = diasFlags or 64
                    if (diasFlags == 0) {
                        Toast.makeText(this@AdicionarHorarioActivity, "Selecione pelo menos um dia da semana", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // Lógica de envio (já a tinha, mantemos)
                    val schedules = viewModel.schedules.value
                    val usedIndexes = schedules.map { it.index }.toSet()
                    var nextIndex = 0
                    while (usedIndexes.contains(nextIndex)) {
                        nextIndex++
                    }
                    val newSchedule = Schedule(nextIndex, horaInicial, minutosInicial, horaFinal, minutosFinal, diasFlags)
                    viewModel.saveSchedule(newSchedule)

                    Toast.makeText(this@AdicionarHorarioActivity, "Agendamento enviado com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()

                } finally {
                    // Garante que o diálogo de progresso é sempre fechado
                    progressDialog.dismiss()
                }
            }
        }
    }

    private fun mostrarTimePickerHorarioInicial(btn: Button) {
        val picker = com.google.android.material.timepicker.MaterialTimePicker.Builder()
            .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
            .setHour(horaInicial)
            .setMinute(minutosInicial)
            .setTitleText("Selecionar Horário Inicial")
            .build()
        picker.addOnPositiveButtonClickListener {
            horaInicial = picker.hour
            minutosInicial = picker.minute
            btn.text = String.format("%02d:%02d", horaInicial, minutosInicial)
        }
        picker.show(supportFragmentManager, "timePickerInicial")
    }

    private fun mostrarTimePickerHorarioFinal(btn: Button) {
        val picker = com.google.android.material.timepicker.MaterialTimePicker.Builder()
            .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
            .setHour(horaFinal)
            .setMinute(minutosFinal)
            .setTitleText("Selecionar Horário Final")
            .build()
        picker.addOnPositiveButtonClickListener {
            horaFinal = picker.hour
            minutosFinal = picker.minute
            btn.text = String.format("%02d:%02d", horaFinal, minutosFinal)
        }
        picker.show(supportFragmentManager, "timePickerFinal")
    }
}
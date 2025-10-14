package com.example.extensaotelas

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class EditarHorarioActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    private var indice: Int = -1
    private var horaInicial: Int = 0
    private var minutosInicial: Int = 0
    private var horaFinal: Int = 0
    private var minutosFinal: Int = 0
    private var diasFlags: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_horario)

        indice = intent.getIntExtra("index", -1)
        horaInicial = intent.getIntExtra("startHour", 0)
        minutosInicial = intent.getIntExtra("startMinute", 0)
        horaFinal = intent.getIntExtra("endHour", 0)
        minutosFinal = intent.getIntExtra("endMinute", 0)
        diasFlags = intent.getIntExtra("daysFlags", 0)
        if (indice == -1) {
            finish()
            return
        }

        val btnHorarioInicial = findViewById<Button>(R.id.btnEditarHorarioInicial)
        val btnHorarioFinal = findViewById<Button>(R.id.btnEditarHorarioFinal)
        val btnSalvar = findViewById<Button>(R.id.btnSalvarEdicaoHorario)

        btnHorarioInicial.text = String.format("%02d:%02d", horaInicial, minutosInicial)
        btnHorarioFinal.text = String.format("%02d:%02d", horaFinal, minutosFinal)

        btnHorarioInicial.setOnClickListener {
            mostrarTimePickerHorarioInicial(btnHorarioInicial)
        }
        btnHorarioFinal.setOnClickListener {
            mostrarTimePickerHorarioFinal(btnHorarioFinal)
        }
        btnSalvar.setOnClickListener {
            val schedule = Schedule(
                index = indice,
                startHour = horaInicial,
                startMinute = minutosInicial,
                endHour = horaFinal,
                endMinute = minutosFinal,
                daysFlags = diasFlags
            )
            viewModel.saveSchedule(schedule)
            Toast.makeText(this, "Atualizado no Arduino", Toast.LENGTH_SHORT).show()
            finish()
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
        picker.show(supportFragmentManager, "timePickerInicialEdit")
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
        picker.show(supportFragmentManager, "timePickerFinalEdit")
    }
} 
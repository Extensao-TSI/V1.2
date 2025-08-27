package com.example.extensaotelas

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.extensaotelas.BancoDeDados.AppDatabase
import com.example.extensaotelas.BancoDeDados.Horario
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditarHorarioActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private var horario: Horario? = null
    private var horaInicial: Int = 0
    private var minutosInicial: Int = 0
    private var horaFinal: Int = 0
    private var minutosFinal: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_horario)

        db = androidx.room.Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).build()

        val idHorario = intent.getIntExtra("idHorario", -1)
        if (idHorario == -1) {
            finish()
            return
        }

        val btnHorarioInicial = findViewById<Button>(R.id.btnEditarHorarioInicial)
        val btnHorarioFinal = findViewById<Button>(R.id.btnEditarHorarioFinal)
        val btnSalvar = findViewById<Button>(R.id.btnSalvarEdicaoHorario)

        lifecycleScope.launch {
            horario = withContext(Dispatchers.IO) {
                db.horarioDao().getById(idHorario)
            }
            horario?.let {
                horaInicial = it.horaInicial
                minutosInicial = it.minutosInicial
                horaFinal = it.horaFinal
                minutosFinal = it.minutosFinal
                btnHorarioInicial.text = String.format("%02d:%02d", horaInicial, minutosInicial)
                btnHorarioFinal.text = String.format("%02d:%02d", horaFinal, minutosFinal)
            }
        }

        btnHorarioInicial.setOnClickListener {
            mostrarTimePickerHorarioInicial(btnHorarioInicial)
        }
        btnHorarioFinal.setOnClickListener {
            mostrarTimePickerHorarioFinal(btnHorarioFinal)
        }
        btnSalvar.setOnClickListener {
            horario?.let { h ->
                h.horaInicial = horaInicial
                h.minutosInicial = minutosInicial
                h.horaFinal = horaFinal
                h.minutosFinal = minutosFinal
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db.horarioDao().updateHorario(h)
                    }
                    finish()
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
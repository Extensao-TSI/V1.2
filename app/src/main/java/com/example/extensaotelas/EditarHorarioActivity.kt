package com.example.extensaotelas

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
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

    private lateinit var edDom: CheckBox
    private lateinit var edSeg: CheckBox
    private lateinit var edTer: CheckBox
    private lateinit var edQua: CheckBox
    private lateinit var edQui: CheckBox
    private lateinit var edSex: CheckBox
    private lateinit var edSab: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_horario)

        indice = intent.getIntExtra("index", -1)
        horaInicial = intent.getIntExtra("startHour", 0)
        minutosInicial = intent.getIntExtra("startMinute", 0)
        horaFinal = intent.getIntExtra("endHour", 0)
        minutosFinal = intent.getIntExtra("endMinute", 0)
        diasFlags = intent.getIntExtra("daysFlags", 0)
        edDom = findViewById(R.id.cbDom)
        edSeg = findViewById(R.id.cbSeg)
        edTer = findViewById(R.id.cbTer)
        edQua = findViewById(R.id.cbQua)
        edQui = findViewById(R.id.cbQui)
        edSex = findViewById(R.id.cbSex)
        edSab = findViewById(R.id.cbSab)
        if (indice == -1) {
            finish()
            return
        }

        populateViewsWithInitialData()

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
            diasFlags = 0
            if (edDom.isChecked) diasFlags = diasFlags or 1
            if (edSeg.isChecked) diasFlags = diasFlags or 2
            if (edTer.isChecked) diasFlags = diasFlags or 4
            if (edQua.isChecked) diasFlags = diasFlags or 8
            if (edQui.isChecked) diasFlags = diasFlags or 16
            if (edSex.isChecked) diasFlags = diasFlags or 32
            if (edSab.isChecked) diasFlags = diasFlags or 64
            if (diasFlags == 0) {
                Toast.makeText(this@EditarHorarioActivity, "Selecione pelo menos um dia da semana", Toast.LENGTH_SHORT).show()
            }

            val schedule = Schedule(
                indice,
                horaInicial,
                minutosInicial,
                horaFinal,
                minutosFinal,
                diasFlags
            )
            Log.e("Schedule", "onCreate: $schedule")

            viewModel.saveSchedule(schedule)
            Log.e("STATUS", "200:OK")
            Toast.makeText(this, "Atualizado no Arduino", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    private fun populateViewsWithInitialData() {
        edDom.isChecked = (diasFlags and 1) != 0
        edSeg.isChecked = (diasFlags and 2) != 0
        edTer.isChecked = (diasFlags and 4) != 0
        edQua.isChecked = (diasFlags and 8) != 0
        edQui.isChecked = (diasFlags and 16) != 0
        edSex.isChecked = (diasFlags and 32) != 0
        edSab.isChecked = (diasFlags and 64) != 0
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
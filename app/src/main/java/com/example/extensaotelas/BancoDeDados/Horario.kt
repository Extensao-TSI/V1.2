package com.example.extensaotelas.BancoDeDados

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Horario (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var ano: Int = 0,
    var mes: Int = 0,
    var dia: Int = 0,
    var horaInicial: Int,
    var minutosInicial: Int,
    var horaFinal: Int,
    var minutosFinal: Int,
    var ativo: Boolean = true
)

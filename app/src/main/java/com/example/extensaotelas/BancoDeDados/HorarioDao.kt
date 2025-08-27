package com.example.extensaotelas.BancoDeDados

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface HorarioDao {

    @Insert
    fun insertHorario(vararg horario: Horario)

    @Delete
    fun deleteHorario(vararg horario: Horario)

    @Query("SELECT * FROM Horario")
    fun getAll(): List<Horario>

    @Query("SELECT * FROM Horario WHERE id = :id")
    fun getById(id: Int): Horario?

    @Update
    fun updateHorario(horario: Horario)

    @Query("UPDATE Horario SET ativo = :ativo WHERE id = :id")
    fun setAtivo(id: Int, ativo: Boolean)
}
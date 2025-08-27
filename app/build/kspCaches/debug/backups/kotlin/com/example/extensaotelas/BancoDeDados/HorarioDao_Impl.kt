package com.example.extensaotelas.BancoDeDados

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class HorarioDao_Impl(
  __db: RoomDatabase,
) : HorarioDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfHorario: EntityInsertAdapter<Horario>

  private val __deleteAdapterOfHorario: EntityDeleteOrUpdateAdapter<Horario>

  private val __updateAdapterOfHorario: EntityDeleteOrUpdateAdapter<Horario>
  init {
    this.__db = __db
    this.__insertAdapterOfHorario = object : EntityInsertAdapter<Horario>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `Horario` (`id`,`ano`,`mes`,`dia`,`horaInicial`,`minutosInicial`,`horaFinal`,`minutosFinal`,`ativo`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Horario) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindLong(2, entity.ano.toLong())
        statement.bindLong(3, entity.mes.toLong())
        statement.bindLong(4, entity.dia.toLong())
        statement.bindLong(5, entity.horaInicial.toLong())
        statement.bindLong(6, entity.minutosInicial.toLong())
        statement.bindLong(7, entity.horaFinal.toLong())
        statement.bindLong(8, entity.minutosFinal.toLong())
        val _tmp: Int = if (entity.ativo) 1 else 0
        statement.bindLong(9, _tmp.toLong())
      }
    }
    this.__deleteAdapterOfHorario = object : EntityDeleteOrUpdateAdapter<Horario>() {
      protected override fun createQuery(): String = "DELETE FROM `Horario` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Horario) {
        statement.bindLong(1, entity.id.toLong())
      }
    }
    this.__updateAdapterOfHorario = object : EntityDeleteOrUpdateAdapter<Horario>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `Horario` SET `id` = ?,`ano` = ?,`mes` = ?,`dia` = ?,`horaInicial` = ?,`minutosInicial` = ?,`horaFinal` = ?,`minutosFinal` = ?,`ativo` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Horario) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindLong(2, entity.ano.toLong())
        statement.bindLong(3, entity.mes.toLong())
        statement.bindLong(4, entity.dia.toLong())
        statement.bindLong(5, entity.horaInicial.toLong())
        statement.bindLong(6, entity.minutosInicial.toLong())
        statement.bindLong(7, entity.horaFinal.toLong())
        statement.bindLong(8, entity.minutosFinal.toLong())
        val _tmp: Int = if (entity.ativo) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        statement.bindLong(10, entity.id.toLong())
      }
    }
  }

  public override fun insertHorario(vararg horario: Horario): Unit = performBlocking(__db, false,
      true) { _connection ->
    __insertAdapterOfHorario.insert(_connection, horario)
  }

  public override fun deleteHorario(vararg horario: Horario): Unit = performBlocking(__db, false,
      true) { _connection ->
    __deleteAdapterOfHorario.handleMultiple(_connection, horario)
  }

  public override fun updateHorario(horario: Horario): Unit = performBlocking(__db, false, true) {
      _connection ->
    __updateAdapterOfHorario.handle(_connection, horario)
  }

  public override fun getAll(): List<Horario> {
    val _sql: String = "SELECT * FROM Horario"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAno: Int = getColumnIndexOrThrow(_stmt, "ano")
        val _columnIndexOfMes: Int = getColumnIndexOrThrow(_stmt, "mes")
        val _columnIndexOfDia: Int = getColumnIndexOrThrow(_stmt, "dia")
        val _columnIndexOfHoraInicial: Int = getColumnIndexOrThrow(_stmt, "horaInicial")
        val _columnIndexOfMinutosInicial: Int = getColumnIndexOrThrow(_stmt, "minutosInicial")
        val _columnIndexOfHoraFinal: Int = getColumnIndexOrThrow(_stmt, "horaFinal")
        val _columnIndexOfMinutosFinal: Int = getColumnIndexOrThrow(_stmt, "minutosFinal")
        val _columnIndexOfAtivo: Int = getColumnIndexOrThrow(_stmt, "ativo")
        val _result: MutableList<Horario> = mutableListOf()
        while (_stmt.step()) {
          val _item: Horario
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpAno: Int
          _tmpAno = _stmt.getLong(_columnIndexOfAno).toInt()
          val _tmpMes: Int
          _tmpMes = _stmt.getLong(_columnIndexOfMes).toInt()
          val _tmpDia: Int
          _tmpDia = _stmt.getLong(_columnIndexOfDia).toInt()
          val _tmpHoraInicial: Int
          _tmpHoraInicial = _stmt.getLong(_columnIndexOfHoraInicial).toInt()
          val _tmpMinutosInicial: Int
          _tmpMinutosInicial = _stmt.getLong(_columnIndexOfMinutosInicial).toInt()
          val _tmpHoraFinal: Int
          _tmpHoraFinal = _stmt.getLong(_columnIndexOfHoraFinal).toInt()
          val _tmpMinutosFinal: Int
          _tmpMinutosFinal = _stmt.getLong(_columnIndexOfMinutosFinal).toInt()
          val _tmpAtivo: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfAtivo).toInt()
          _tmpAtivo = _tmp != 0
          _item =
              Horario(_tmpId,_tmpAno,_tmpMes,_tmpDia,_tmpHoraInicial,_tmpMinutosInicial,_tmpHoraFinal,_tmpMinutosFinal,_tmpAtivo)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getById(id: Int): Horario? {
    val _sql: String = "SELECT * FROM Horario WHERE id = ?"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAno: Int = getColumnIndexOrThrow(_stmt, "ano")
        val _columnIndexOfMes: Int = getColumnIndexOrThrow(_stmt, "mes")
        val _columnIndexOfDia: Int = getColumnIndexOrThrow(_stmt, "dia")
        val _columnIndexOfHoraInicial: Int = getColumnIndexOrThrow(_stmt, "horaInicial")
        val _columnIndexOfMinutosInicial: Int = getColumnIndexOrThrow(_stmt, "minutosInicial")
        val _columnIndexOfHoraFinal: Int = getColumnIndexOrThrow(_stmt, "horaFinal")
        val _columnIndexOfMinutosFinal: Int = getColumnIndexOrThrow(_stmt, "minutosFinal")
        val _columnIndexOfAtivo: Int = getColumnIndexOrThrow(_stmt, "ativo")
        val _result: Horario?
        if (_stmt.step()) {
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpAno: Int
          _tmpAno = _stmt.getLong(_columnIndexOfAno).toInt()
          val _tmpMes: Int
          _tmpMes = _stmt.getLong(_columnIndexOfMes).toInt()
          val _tmpDia: Int
          _tmpDia = _stmt.getLong(_columnIndexOfDia).toInt()
          val _tmpHoraInicial: Int
          _tmpHoraInicial = _stmt.getLong(_columnIndexOfHoraInicial).toInt()
          val _tmpMinutosInicial: Int
          _tmpMinutosInicial = _stmt.getLong(_columnIndexOfMinutosInicial).toInt()
          val _tmpHoraFinal: Int
          _tmpHoraFinal = _stmt.getLong(_columnIndexOfHoraFinal).toInt()
          val _tmpMinutosFinal: Int
          _tmpMinutosFinal = _stmt.getLong(_columnIndexOfMinutosFinal).toInt()
          val _tmpAtivo: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfAtivo).toInt()
          _tmpAtivo = _tmp != 0
          _result =
              Horario(_tmpId,_tmpAno,_tmpMes,_tmpDia,_tmpHoraInicial,_tmpMinutosInicial,_tmpHoraFinal,_tmpMinutosFinal,_tmpAtivo)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun setAtivo(id: Int, ativo: Boolean) {
    val _sql: String = "UPDATE Horario SET ativo = ? WHERE id = ?"
    return performBlocking(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (ativo) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, id.toLong())
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}

package com.example.extensaotelas.database

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class SensorDataDao_Impl(
  __db: RoomDatabase,
) : SensorDataDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSensorDataEntity: EntityInsertAdapter<SensorDataEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfSensorDataEntity = object : EntityInsertAdapter<SensorDataEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `sensor_data` (`id`,`timestamp`,`temperature`,`airHumidity`,`soilHumidity`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SensorDataEntity) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindLong(2, entity.timestamp)
        statement.bindDouble(3, entity.temperature.toDouble())
        statement.bindDouble(4, entity.airHumidity.toDouble())
        statement.bindLong(5, entity.soilHumidity.toLong())
      }
    }
  }

  public override suspend fun insertAll(`data`: List<SensorDataEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSensorDataEntity.insert(_connection, data)
  }

  public override suspend fun insert(`data`: SensorDataEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfSensorDataEntity.insert(_connection, data)
  }

  public override suspend fun getLastTimestamp(): Long? {
    val _sql: String = "SELECT MAX(timestamp) FROM sensor_data"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Long?
        if (_stmt.step()) {
          val _tmp: Long?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllData(): Flow<List<SensorDataEntity>> {
    val _sql: String = "SELECT * FROM sensor_data ORDER BY timestamp ASC"
    return createFlow(__db, false, arrayOf("sensor_data")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfTemperature: Int = getColumnIndexOrThrow(_stmt, "temperature")
        val _columnIndexOfAirHumidity: Int = getColumnIndexOrThrow(_stmt, "airHumidity")
        val _columnIndexOfSoilHumidity: Int = getColumnIndexOrThrow(_stmt, "soilHumidity")
        val _result: MutableList<SensorDataEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SensorDataEntity
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpTemperature: Float
          _tmpTemperature = _stmt.getDouble(_columnIndexOfTemperature).toFloat()
          val _tmpAirHumidity: Float
          _tmpAirHumidity = _stmt.getDouble(_columnIndexOfAirHumidity).toFloat()
          val _tmpSoilHumidity: Int
          _tmpSoilHumidity = _stmt.getLong(_columnIndexOfSoilHumidity).toInt()
          _item =
              SensorDataEntity(_tmpId,_tmpTimestamp,_tmpTemperature,_tmpAirHumidity,_tmpSoilHumidity)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAll() {
    val _sql: String = "DELETE FROM sensor_data"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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

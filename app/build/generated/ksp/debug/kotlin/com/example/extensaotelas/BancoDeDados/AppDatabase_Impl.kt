package com.example.extensaotelas.BancoDeDados

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _horarioDao: Lazy<HorarioDao> = lazy {
    HorarioDao_Impl(this)
  }

  private val _sensorDataDao: Lazy<SensorDataDao> = lazy {
    SensorDataDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(3,
        "e4f4a9282b67caae3ccacb256c9a7e2e", "c3e720ce873338688ab559758796b7d6") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `Horario` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ano` INTEGER NOT NULL, `mes` INTEGER NOT NULL, `dia` INTEGER NOT NULL, `horaInicial` INTEGER NOT NULL, `minutosInicial` INTEGER NOT NULL, `horaFinal` INTEGER NOT NULL, `minutosFinal` INTEGER NOT NULL, `ativo` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sensor_data` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `temperature` REAL NOT NULL, `airHumidity` REAL NOT NULL, `soilHumidity` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e4f4a9282b67caae3ccacb256c9a7e2e')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `Horario`")
        connection.execSQL("DROP TABLE IF EXISTS `sensor_data`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsHorario: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsHorario.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHorario.put("ano", TableInfo.Column("ano", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHorario.put("mes", TableInfo.Column("mes", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHorario.put("dia", TableInfo.Column("dia", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHorario.put("horaInicial", TableInfo.Column("horaInicial", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHorario.put("minutosInicial", TableInfo.Column("minutosInicial", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHorario.put("horaFinal", TableInfo.Column("horaFinal", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHorario.put("minutosFinal", TableInfo.Column("minutosFinal", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHorario.put("ativo", TableInfo.Column("ativo", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysHorario: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesHorario: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoHorario: TableInfo = TableInfo("Horario", _columnsHorario, _foreignKeysHorario,
            _indicesHorario)
        val _existingHorario: TableInfo = read(connection, "Horario")
        if (!_infoHorario.equals(_existingHorario)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |Horario(com.example.extensaotelas.BancoDeDados.Horario).
              | Expected:
              |""".trimMargin() + _infoHorario + """
              |
              | Found:
              |""".trimMargin() + _existingHorario)
        }
        val _columnsSensorData: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSensorData.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSensorData.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSensorData.put("temperature", TableInfo.Column("temperature", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSensorData.put("airHumidity", TableInfo.Column("airHumidity", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSensorData.put("soilHumidity", TableInfo.Column("soilHumidity", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSensorData: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSensorData: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSensorData: TableInfo = TableInfo("sensor_data", _columnsSensorData,
            _foreignKeysSensorData, _indicesSensorData)
        val _existingSensorData: TableInfo = read(connection, "sensor_data")
        if (!_infoSensorData.equals(_existingSensorData)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |sensor_data(com.example.extensaotelas.BancoDeDados.SensorDataEntity).
              | Expected:
              |""".trimMargin() + _infoSensorData + """
              |
              | Found:
              |""".trimMargin() + _existingSensorData)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "Horario", "sensor_data")
  }

  public override fun clearAllTables() {
    super.performClear(false, "Horario", "sensor_data")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(HorarioDao::class, HorarioDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(SensorDataDao::class, SensorDataDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun horarioDao(): HorarioDao = _horarioDao.value

  public override fun sensorDataDao(): SensorDataDao = _sensorDataDao.value
}

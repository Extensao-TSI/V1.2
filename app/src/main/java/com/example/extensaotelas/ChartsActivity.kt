package com.example.extensaotelas

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.extensaotelas.BancoDeDados.AppDatabase
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChartsActivity : AppCompatActivity() {

    private lateinit var chartTemperatura: com.github.mikephil.charting.charts.LineChart
    private lateinit var chartUmidadeAr: com.github.mikephil.charting.charts.LineChart
    private lateinit var chartUmidadeSolo: com.github.mikephil.charting.charts.LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charts)

        chartTemperatura = findViewById(R.id.chartTemperatura)
        chartUmidadeAr = findViewById(R.id.chartUmidadeAr)
        chartUmidadeSolo = findViewById(R.id.chartUmidadeSolo)

        setupChart(chartTemperatura)
        setupChart(chartUmidadeAr)
        setupChart(chartUmidadeSolo)

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabRefresh).setOnClickListener {
            android.widget.Toast.makeText(this, "Atualizando dados...", android.widget.Toast.LENGTH_SHORT).show()
            loadData()
        }

        loadData()
    }

    private fun setupChart(chart: com.github.mikephil.charting.charts.LineChart) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.legend.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.DKGRAY
        xAxis.valueFormatter = object : ValueFormatter() {
            private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                return sdf.format(Date(value.toLong() * 1000)) // Assumindo timestamp em segundos
            }
        }

        val yAxisLeft = chart.axisLeft
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.textColor = Color.DKGRAY

        val yAxisRight = chart.axisRight
        yAxisRight.isEnabled = false
    }

    private fun loadData() {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@ChartsActivity).sensorDataDao()
            val allData = dao.getAllSensorDataFlow().first()
            if (allData.isEmpty()) return@launch

            // Filtragem de Outliers: Removemos pontos que fogem drasticamente da cronologia principal
            // Baseamos no último timestamp (mais recente) e pegamos apenas o que está em um intervalo razoável (ex: últimas 48h)
            val latestTs = allData.maxOf { it.timestamp }
            val dataList = allData.filter { it.timestamp > (latestTs - 48 * 3600) && it.timestamp <= latestTs }
                .sortedBy { it.timestamp }

            if (dataList.isEmpty()) return@launch

            val minTimestamp = dataList.first().timestamp
            val tempEntries = ArrayList<Entry>()
            val umidArEntries = ArrayList<Entry>()
            val umidSoloEntries = ArrayList<Entry>()

            for (data in dataList) {
                val xVal = (data.timestamp - minTimestamp).toFloat()
                tempEntries.add(Entry(xVal, data.temperature))
                umidArEntries.add(Entry(xVal, data.airHumidity))
                umidSoloEntries.add(Entry(xVal, data.soilHumidity.toFloat()))
            }

            val formatter = object : ValueFormatter() {
                private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                override fun getFormattedValue(value: Float): String {
                    val actualTimestamp = (value.toLong() + minTimestamp) * 1000
                    return sdf.format(Date(actualTimestamp))
                }
            }
            
            chartTemperatura.xAxis.valueFormatter = formatter
            chartUmidadeAr.xAxis.valueFormatter = formatter
            chartUmidadeSolo.xAxis.valueFormatter = formatter

            // Configuração dos Datasets como Linhas
            setupLineChartData(chartTemperatura, tempEntries, "Temperatura", "#E74C3C")
            setupLineChartData(chartUmidadeAr, umidArEntries, "Umidade Ar", "#3498DB")
            setupLineChartData(chartUmidadeSolo, umidSoloEntries, "Umidade Solo", "#27AE60")
        }
    }

    private fun setupLineChartData(chart: com.github.mikephil.charting.charts.LineChart, entries: List<Entry>, label: String, colorHex: String) {
        val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, label)
        dataSet.color = Color.parseColor(colorHex)
        dataSet.setCircleColor(Color.parseColor(colorHex))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 3f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 0f // Ocultar valores nos pontos para não poluir
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor(colorHex)
        dataSet.fillAlpha = 50
        
        // Suavizar a linha
        dataSet.mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
        
        chart.data = com.github.mikephil.charting.data.LineData(dataSet)
        chart.invalidate()
    }
}

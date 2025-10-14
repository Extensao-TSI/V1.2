package com.example.extensaotelas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch

class HorarioAdapter(
    var schedules: MutableList<Schedule>,
    val onEditar: (Schedule) -> Unit,
    val onExcluir: (Schedule) -> Unit
) : RecyclerView.Adapter<HorarioAdapter.MyViewHolder>() {

    // Mapa das flags para os nomes dos dias
    private val dayMap = mapOf(
        1 to "Dom", 2 to "Seg", 4 to "Ter", 8 to "Qua",
        16 to "Qui", 32 to "Sex", 64 to "Sab"
    )

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewHorario: TextView = itemView.findViewById(R.id.textViewHorario)
        val btnEditar: Button = itemView.findViewById(R.id.btnEditar)
        val btnExcluir: Button = itemView.findViewById(R.id.btnExcluir)
        val btnToggle: MaterialSwitch = itemView.findViewById(R.id.btnToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_horario, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val schedule = schedules[position]
        val horaInicial = String.format("%02d:%02d", schedule.startHour, schedule.startMinute)
        val horaFinal = String.format("%02d:%02d", schedule.endHour, schedule.endMinute)

        // Converte a flag de dias para uma string legível
        val diasAtivos = dayMap.keys
            .filter { (schedule.daysFlags and it) != 0 }
            .joinToString(" ") { dayMap[it] ?: "" }

        holder.textViewHorario.text = "Índice ${schedule.index} | $horaInicial - $horaFinal | $diasAtivos"

        holder.btnEditar.setOnClickListener { onEditar(schedule) }
        holder.btnExcluir.setOnClickListener { onExcluir(schedule) }
        // O switch não tem mais função aqui, pois o Arduino controla o estado
        holder.btnToggle.visibility = View.GONE
    }

    override fun getItemCount() = schedules.size
}
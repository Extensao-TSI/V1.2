package com.example.extensaotelas


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.example.extensaotelas.BancoDeDados.Horario

class HorarioAdapter(
    var horarios: MutableList<Horario>,
    val onEditar: (Horario) -> Unit,
    val onExcluir: (Horario) -> Unit,
    val onToggle: (Horario, Boolean) -> Unit
) : RecyclerView.Adapter<HorarioAdapter.MyViewHolder>() {

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
        val horario = horarios[position]
        val horaInicial = String.format("%02d:%02d", horario.horaInicial, horario.minutosInicial)
        val horaFinal = String.format("%02d:%02d", horario.horaFinal, horario.minutosFinal)
        val data = if (horario.dia > 0 && horario.mes > 0 && horario.ano > 0) String.format("%02d/%02d/%04d", horario.dia, horario.mes, horario.ano) else "Sem data"
        holder.textViewHorario.text = "$data | $horaInicial - $horaFinal"

        holder.btnEditar.setOnClickListener {
            onEditar(horario)
        }

        holder.btnExcluir.setOnClickListener {
            onExcluir(horario)
        }

        holder.btnToggle.isChecked = horario.ativo
        holder.btnToggle.setOnCheckedChangeListener { _, isChecked ->
            onToggle(horario, isChecked)
        }
    }

    override fun getItemCount(): Int {
        return horarios.size
    }
} 
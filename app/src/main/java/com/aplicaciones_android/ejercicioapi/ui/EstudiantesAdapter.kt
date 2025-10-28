package com.aplicaciones_android.ejercicioapi.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aplicaciones_android.ejercicioapi.R
import com.aplicaciones_android.ejercicioapi.model.Estudiante

class EstudiantesAdapter(
    private var items: List<Estudiante>,
    var onDeleteClick: ((Estudiante) -> Unit)? = null,
    var onEditClick: ((Estudiante) -> Unit)? = null,
    private var cursosMap: Map<Int, String>? = null
) : RecyclerView.Adapter<EstudiantesAdapter.VH>() {

    // Lista completa para filtrar
    private var allItems: List<Estudiante> = items.toList()

    fun update(newItems: List<Estudiante>) {
        allItems = newItems.toList()
        items = newItems
        notifyDataSetChanged()
    }

    fun removeById(id: Int) {
        allItems = allItems.filter { it.id != id }
        items = items.filter { it.id != id }
        notifyDataSetChanged()
    }

    /**
     * Actualiza (reemplaza) un estudiante existente por id en las listas internas.
     * Si el estudiante está visible en la lista actual (`items`) notifica el cambio
     * usando notifyItemChanged(pos) para una actualización eficiente.
     */
    fun updateItem(updated: Estudiante) {
        // Actualizar allItems
        val allIdx = allItems.indexOfFirst { it.id == updated.id }
        if (allIdx >= 0) {
            val tmpAll = allItems.toMutableList()
            tmpAll[allIdx] = updated
            allItems = tmpAll
        }

        // Actualizar items (lista mostrada) y notificar cambio si está visible
        val idx = items.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            val tmp = items.toMutableList()
            tmp[idx] = updated
            items = tmp
            notifyItemChanged(idx)
        }
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            items = allItems
        } else {
            items = allItems.filter { e ->
                val idStr = e.id.toString()
                val nombre = e.nombre?.lowercase() ?: ""
                val apellido = e.apellido?.lowercase() ?: ""
                val email = e.email?.lowercase() ?: ""
                val github = e.githubUrl?.lowercase() ?: ""
                val cursosDetalle = when {
                    !e.cursos.isNullOrEmpty() -> e.cursos.joinToString(",") { it.nombre?.lowercase().orEmpty() }
                    !e.cursosInscritosNombres.isNullOrEmpty() -> e.cursosInscritosNombres.joinToString(",") { it.lowercase() }
                    !e.cursosInscritos.isNullOrEmpty() -> e.cursosInscritos.joinToString(",") { id -> cursosMap?.get(id)?.lowercase() ?: id.toString() }
                    else -> ""
                }
                idStr.contains(q) || nombre.contains(q) || apellido.contains(q) || email.contains(q) || github.contains(q) || cursosDetalle.contains(q)
            }
        }
        notifyDataSetChanged()
    }

    fun setCursosMap(map: Map<Int, String>?) {
        cursosMap = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_estudiante, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        val ctx = holder.itemView.context

        holder.studentId.text = ctx.getString(R.string.label_id, e.id)
        holder.name.text = ctx.getString(
            R.string.label_full_name,
            e.nombre.orEmpty(),
            e.apellido.orEmpty()
        ).trim()
        holder.email.text = ctx.getString(R.string.label_email, e.email.orEmpty())
        holder.github.text = ctx.getString(R.string.label_github, e.githubUrl.orEmpty())

        val cursosStr = when {
            // Si la API nos da detalles de cursos, usar nombre o id
            !e.cursos.isNullOrEmpty() -> e.cursos.joinToString(", ") { cr -> (cr.nombre?.takeIf { it.isNotBlank() } ?: cr.id.toString()).trim() }
            // Si la API nos da nombres directos en cursos_inscritos
            !e.cursosInscritosNombres.isNullOrEmpty() -> e.cursosInscritosNombres.joinToString(", ") { it.trim() }
            // Si solo tenemos ids, mapear a nombres cuando sea posible
            !e.cursosInscritos.isNullOrEmpty() -> {
                val map = cursosMap
                if (map.isNullOrEmpty()) e.cursosInscritos.joinToString(", ") { it.toString() }
                else e.cursosInscritos.joinToString(", ") { id -> (map[id] ?: id.toString()).trim() }
            }
            else -> ""
        }
        holder.cursos.text = ctx.getString(R.string.label_cursos, cursosStr)

        holder.details.visibility = View.GONE

        holder.btnDelete.setOnClickListener { onDeleteClick?.invoke(e) }
        holder.btnEdit.setOnClickListener { onEditClick?.invoke(e) }
        // Permitir editar haciendo click en todo el item
        holder.itemView.setOnClickListener { onEditClick?.invoke(e) }
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val studentId: TextView = itemView.findViewById(R.id.studentId)
        val name: TextView = itemView.findViewById(R.id.name)
        val email: TextView = itemView.findViewById(R.id.email)
        val github: TextView = itemView.findViewById(R.id.github)
        val cursos: TextView = itemView.findViewById(R.id.cursos)
        val details: TextView = itemView.findViewById(R.id.details)
    }
}

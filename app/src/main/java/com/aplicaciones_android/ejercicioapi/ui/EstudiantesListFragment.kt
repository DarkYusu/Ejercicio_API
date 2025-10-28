package com.aplicaciones_android.ejercicioapi.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aplicaciones_android.ejercicioapi.R
import com.aplicaciones_android.ejercicioapi.model.Estudiante
import com.aplicaciones_android.ejercicioapi.model.EstudianteUpdateRequest
import com.aplicaciones_android.ejercicioapi.network.RetrofitClient
import kotlinx.coroutines.launch
import org.json.JSONObject

class EstudiantesListFragment : Fragment() {

    private lateinit var adapter: EstudiantesAdapter
    private var cursosMap: Map<Int, String>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_estudiantes_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler)
        val progress = view.findViewById<View>(R.id.progress)
        val search = view.findViewById<SearchView>(R.id.search)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = EstudiantesAdapter(emptyList(), { estudiante ->
            confirmAndDelete(estudiante.id, progress)
        }, { estudiante ->
            showEditDialog(estudiante, progress)
        }, cursosMap)
        recycler.adapter = adapter

        // Cargar estudiantes (no consultamos la API de cursos)
        loadEstudiantes(progress)

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val q = query ?: ""
                handleSearchQuery(q, progress)
                search.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val q = newText ?: ""
                if (q.isEmpty()) loadEstudiantes(progress) else {
                    val id = q.toIntOrNull()
                    if (id != null) findEstudianteById(id, progress) else adapter.filter(q)
                }
                return true
            }
        })
    }

    private fun confirmAndDelete(id: Int, progress: View) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_message, id))
            .setPositiveButton(R.string.delete_ok) { dialog, _ ->
                dialog.dismiss()
                doDelete(id, progress)
            }
            .setNegativeButton(R.string.delete_cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun doDelete(id: Int, progress: View) {
        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.deleteEstudiante(id)
                if (resp.isSuccessful) {
                    adapter.removeById(id)
                    Toast.makeText(requireContext(), getString(R.string.delete_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), getString(R.string.delete_error), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), getString(R.string.delete_error), Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun handleSearchQuery(q: String, progress: View) {
        if (q.isEmpty()) { loadEstudiantes(progress); return }
        val id = q.toIntOrNull()
        if (id != null) findEstudianteById(id, progress) else adapter.filter(q)
    }

    private fun findEstudianteById(id: Int, progress: View) {
        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getEstudiante(id)
                if (resp.isSuccessful) {
                    val e = resp.body()
                    adapter.update(if (e != null) listOf(e) else emptyList())
                } else {
                    handleError(resp.errorBody()?.string())
                    adapter.update(emptyList())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                adapter.update(emptyList())
            } finally { progress.visibility = View.GONE }
        }
    }

    private fun loadEstudiantes(progress: View) {
        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getEstudiantes()
                if (resp.isSuccessful) {
                    adapter.update(resp.body() ?: emptyList())
                } else {
                    handleError(resp.errorBody()?.string())
                    adapter.update(emptyList())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                adapter.update(emptyList())
            } finally { progress.visibility = View.GONE }
        }
    }

    private fun handleError(errorBody: String?) {
        var detailMsg: String? = null
        if (!errorBody.isNullOrEmpty()) {
            try { val jo = JSONObject(errorBody); if (jo.has("detail")) detailMsg = jo.optString("detail") } catch (_: Exception) {}
        }
        if (!detailMsg.isNullOrEmpty()) Toast.makeText(requireContext(), detailMsg, Toast.LENGTH_LONG).show()
        else Toast.makeText(requireContext(), "Error", Toast.LENGTH_LONG).show()
    }

    private fun showEditDialog(estudiante: Estudiante, progress: View) {
        val ctx = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_estudiante, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombre)
        val etApellido = dialogView.findViewById<EditText>(R.id.etApellido)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etGithub = dialogView.findViewById<EditText>(R.id.etGithub)
        val etCursos = dialogView.findViewById<EditText>(R.id.etCursos)

        etNombre.setText(estudiante.nombre.orEmpty())
        etApellido.setText(estudiante.apellido.orEmpty())
        etEmail.setText(estudiante.email.orEmpty())
        etGithub.setText(estudiante.githubUrl.orEmpty())

        // Prefill cursos con nombres si es posible
        val map = cursosMap.orEmpty()
        val prefillCursos = when {
            !estudiante.cursos.isNullOrEmpty() -> estudiante.cursos.joinToString(", ") { it.nombre?.takeIf { n -> n.isNotBlank() } ?: it.id.toString() }
            !estudiante.cursosInscritosNombres.isNullOrEmpty() -> estudiante.cursosInscritosNombres.joinToString(", ") { it.trim() }
            !estudiante.cursosInscritos.isNullOrEmpty() -> estudiante.cursosInscritos.joinToString(", ") { id -> map[id] ?: id.toString() }
            else -> ""
        }
        etCursos.setText(prefillCursos)

        AlertDialog.Builder(ctx)
            .setTitle("Editar estudiante #${estudiante.id}")
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar) { d, _ ->
                d.dismiss()
                val nombreIn = etNombre.text.toString().trim().ifEmpty { null }
                val apellidoIn = etApellido.text.toString().trim().ifEmpty { null }
                val emailIn = etEmail.text.toString().trim().ifEmpty { null }
                val githubIn = etGithub.text.toString().trim().ifEmpty { null }

                // Parsear cursos: aceptar siempre texto libre. Si existe `map` y encontramos
                // un nombre oficial para un id, lo usamos; en caso contrario enviamos el token tal cual.
                val cursosStr = etCursos.text.toString().trim()
                val tokens = cursosStr.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                val resolvedNames = mutableListOf<String>()
                if (tokens.isNotEmpty()) {
                    // Si no tenemos mapeo, aceptamos tokens tal cual
                    if (map.isEmpty()) {
                        resolvedNames.addAll(tokens)
                    } else {
                        // map: id -> nombre
                        for (tk in tokens) {
                            val asId = tk.toIntOrNull()
                            if (asId != null) {
                                val name = map[asId]
                                if (!name.isNullOrEmpty()) resolvedNames.add(name) else resolvedNames.add(tk)
                            } else {
                                val match = map.values.firstOrNull { it.equals(tk, ignoreCase = true) }
                                if (match != null) resolvedNames.add(match) else resolvedNames.add(tk)
                            }
                        }
                    }
                }

                val nombre = nombreIn ?: estudiante.nombre ?: ""
                val apellido = apellidoIn ?: estudiante.apellido ?: ""
                val email = emailIn ?: estudiante.email ?: ""
                val github = githubIn ?: estudiante.githubUrl ?: ""

                val cursosNames: List<String> = if (tokens.isEmpty()) {
                    when {
                        !estudiante.cursosInscritosNombres.isNullOrEmpty() -> estudiante.cursosInscritosNombres
                        !estudiante.cursos.isNullOrEmpty() -> estudiante.cursos.map { it.nombre ?: it.id.toString() }
                        !estudiante.cursosInscritos.isNullOrEmpty() -> estudiante.cursosInscritos.map { id -> map[id] ?: id.toString() }
                        else -> emptyList()
                    }
                } else resolvedNames

                val body = EstudianteUpdateRequest(
                    id = estudiante.id,
                    nombre = nombre,
                    apellido = apellido,
                    email = email,
                    githubUrl = github,
                    cursosInscritos = cursosNames
                )
                doUpdate(estudiante.id, body, progress)
            }
            .setNegativeButton(R.string.delete_cancel, null)
            .show()
    }

    private fun doUpdate(id: Int, body: EstudianteUpdateRequest, progress: View) {
        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.updateEstudiante(id, body)
                if (resp.isSuccessful) {
                    // Recargar la lista para reflejar cambios en la UI
                    loadEstudiantes(progress)
                    Toast.makeText(requireContext(), "Estudiante actualizado", Toast.LENGTH_SHORT).show()
                } else {
                    handleError(resp.errorBody()?.string())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }
}

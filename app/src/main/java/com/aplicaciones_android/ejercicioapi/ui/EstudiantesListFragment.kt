package com.aplicaciones_android.ejercicioapi.ui

import android.os.Bundle
import android.util.Log
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
import org.json.JSONArray
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

        // Cargar cursos y luego estudiantes
        loadCursosThenEstudiantes(progress)

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

    private fun loadCursosThenEstudiantes(progress: View) {
        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val cursosResp = RetrofitClient.api.getCursos()
                if (cursosResp.isSuccessful) {
                    val cursos = cursosResp.body() ?: emptyList()
                    cursosMap = cursos.associate { it.id to (it.nombre ?: "Curso ${it.id}") }
                    adapter.setCursosMap(cursosMap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Tras intentar cargar cursos, cargar estudiantes
                loadEstudiantes(progress)
            }
        }
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
            !estudiante.cursosInscritosNombres.isNullOrEmpty() -> estudiante.cursosInscritosNombres?.joinToString(", ") { it.trim() }
            !estudiante.cursosInscritos.isNullOrEmpty() -> estudiante.cursosInscritos?.joinToString(", ") { id -> map[id] ?: id.toString() }
            else -> ""
        }
        etCursos.setText(prefillCursos ?: "")

        AlertDialog.Builder(ctx)
            .setTitle("Editar estudiante #${estudiante.id}")
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar) { d, _ ->
                d.dismiss()
                val nombreIn = etNombre.text.toString().trim().ifEmpty { null }
                val apellidoIn = etApellido.text.toString().trim().ifEmpty { null }
                val emailIn = etEmail.text.toString().trim().ifEmpty { null }
                val githubIn = etGithub.text.toString().trim().ifEmpty { null }

                // Parsear cursos a nombres
                val cursosStr = etCursos.text.toString().trim()
                val tokens = cursosStr.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                val byName = map.mapKeys { it.value.lowercase() } // nombre->id invertido por nombre en minúsculas
                val byId = map // id->nombre
                val resolvedNames = mutableListOf<String>()
                val unknown = mutableListOf<String>()
                for (tk in tokens) {
                    val asId = tk.toIntOrNull()
                    if (asId != null) {
                        val name = byId[asId]
                        if (name != null) resolvedNames.add(name) else unknown.add(tk)
                    } else {
                        val name = tk
                        if (byName.containsKey(name.lowercase())) resolvedNames.add(name) else unknown.add(tk)
                    }
                }

                if (tokens.isNotEmpty() && resolvedNames.isEmpty()) {
                    Toast.makeText(requireContext(), "Cursos no reconocidos: " + unknown.joinToString(", "), Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val nombre = nombreIn ?: estudiante.nombre ?: ""
                val apellido = apellidoIn ?: estudiante.apellido ?: ""
                val email = emailIn ?: estudiante.email ?: ""
                val github = githubIn ?: estudiante.githubUrl ?: ""
                val cursosNames = if (tokens.isEmpty()) {
                    // Si no se editaron cursos, mantener los existentes como nombres cuando sea posible
                    when {
                        !estudiante.cursosInscritosNombres.isNullOrEmpty() -> estudiante.cursosInscritosNombres
                        !estudiante.cursos.isNullOrEmpty() -> estudiante.cursos?.map { it.nombre ?: it.id.toString() }
                        !estudiante.cursosInscritos.isNullOrEmpty() -> estudiante.cursosInscritos?.map { id -> map[id] ?: id.toString() }
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
                    val listResp = RetrofitClient.api.getEstudiantes()
                    if (listResp.isSuccessful) {
                        adapter.update(listResp.body() ?: emptyList())
                    }
                    Toast.makeText(requireContext(), getString(R.string.update_success), Toast.LENGTH_SHORT).show()
                } else {
                    val code = resp.code()
                    val raw = resp.errorBody()?.string()
                    Log.e("PUT_UPDATE", "HTTP $code error body: $raw")
                    val detail = parseUpdateError(raw)
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.title_error_update)
                        .setMessage(getString(R.string.msg_error_update_detalle, code, detail))
                        .setPositiveButton(R.string.btn_cerrar, null)
                        .show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun parseUpdateError(raw: String?): String {
        if (raw.isNullOrEmpty()) return getString(R.string.update_fail)
        try {
            val jo = JSONObject(raw)
            if (jo.has("detail")) return jo.optString("detail")
            val arr = JSONArray(raw)
            if (arr.length() > 0) {
                val msgs = (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.optString("msg") }
                if (msgs.isNotEmpty()) return msgs.joinToString("\n")
            }
        } catch (_: Exception) {}
        return raw.take(500)
    }
}

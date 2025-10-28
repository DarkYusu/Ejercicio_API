package com.aplicaciones_android.ejercicioapi.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aplicaciones_android.ejercicioapi.R
import com.aplicaciones_android.ejercicioapi.model.EstudianteCreateRequest
import com.aplicaciones_android.ejercicioapi.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class EstudianteAddFragment : Fragment() {

    private var cursosByName: Map<String, Int> = emptyMap()
    private var cursosById: Map<Int, String> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_estudiante_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tilId = view.findViewById<TextInputLayout>(R.id.tilId)
        val etId = view.findViewById<TextInputEditText>(R.id.etId)
        val etNombre = view.findViewById<TextInputEditText>(R.id.etNombre)
        val etApellido = view.findViewById<TextInputEditText>(R.id.etApellido)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etGithub = view.findViewById<TextInputEditText>(R.id.etGithub)
        val etCursos = view.findViewById<TextInputEditText>(R.id.etCursos)
        val btnGuardar = view.findViewById<MaterialButton>(R.id.btnGuardar)
        val progress = view.findViewById<ProgressBar>(R.id.progress)

        // Cargar el catálogo de cursos para admitir nombres
        loadCursosCatalog()

        btnGuardar.setOnClickListener {
            val idText = etId.text?.toString()?.trim().orEmpty()
            val idValue = idText.toIntOrNull()
            if (idText.isEmpty()) {
                tilId.error = getString(R.string.msg_id_obligatorio)
                return@setOnClickListener
            }
            if (idValue == null || idValue <= 0) {
                tilId.error = getString(R.string.msg_id_invalido)
                return@setOnClickListener
            } else {
                tilId.error = null
            }

            val nombre = etNombre.text?.toString()?.trim().orEmpty()
            val apellido = etApellido.text?.toString()?.trim().orEmpty()
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val github = etGithub.text?.toString()?.trim()?.ifEmpty { null }

            // Cursos: aceptar IDs o nombres separados por coma
            val cursosInput = etCursos.text?.toString()?.trim().orEmpty()
            val tokens = cursosInput.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val resolvedNames = mutableListOf<String>()
            val unknownTokens = mutableListOf<String>()
            for (tk in tokens) {
                val asId = tk.toIntOrNull()
                if (asId != null) {
                    val name = cursosById[asId]
                    if (name != null) resolvedNames.add(name) else unknownTokens.add(tk)
                } else {
                    val name = tk
                    val id = cursosByName[name.lowercase()]
                    if (id != null) resolvedNames.add(name) else unknownTokens.add(tk)
                }
            }

            if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.msg_campos_obligatorios), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Si el usuario escribió algo en cursos pero no se resolvió nada, avisar y no continuar
            if (tokens.isNotEmpty() && resolvedNames.isEmpty()) {
                val msg = "Cursos no reconocidos: " + unknownTokens.joinToString(", ")
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val body = EstudianteCreateRequest(
                id = idValue,
                nombre = nombre,
                apellido = apellido,
                email = email,
                githubUrl = github,
                cursosInscritos = resolvedNames
            )

            Log.d("ADD_ESTUDIANTE", "tokens=$tokens resolvedNames=$resolvedNames unknown=$unknownTokens")

            progress.visibility = View.VISIBLE
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val resp = RetrofitClient.api.createEstudiante(body)
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), getString(R.string.msg_creado_ok), Toast.LENGTH_LONG).show()
                        etId.setText("")
                        etNombre.setText("")
                        etApellido.setText("")
                        etEmail.setText("")
                        etGithub.setText("")
                        etCursos.setText("")
                    } else {
                        // Mostrar detalle del error
                        val code = resp.code()
                        val raw = resp.errorBody()?.string()
                        val detail = parseErrorDetail(raw)
                        AlertDialog.Builder(requireContext())
                            .setTitle(R.string.title_error_crear)
                            .setMessage(getString(R.string.msg_error_crear_detalle, code, detail))
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
    }

    private fun loadCursosCatalog() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getCursos()
                if (resp.isSuccessful) {
                    val cursos = resp.body().orEmpty()
                    cursosByName = cursos.mapNotNull { c ->
                        val name = c.nombre?.trim()
                        val id = c.id
                        if (!name.isNullOrEmpty()) name.lowercase() to id else null
                    }.toMap()
                    cursosById = cursos.mapNotNull { c ->
                        val name = c.nombre?.trim()
                        if (!name.isNullOrEmpty()) c.id to name else null
                    }.toMap()
                }
            } catch (_: Exception) {
                // Silencioso
            }
        }
    }

    private fun parseErrorDetail(raw: String?): String {
        if (raw.isNullOrEmpty()) return getString(R.string.msg_creado_fail)
        try {
            // Caso 1: { "detail": "mensaje" }
            val jo = JSONObject(raw)
            if (jo.has("detail")) return jo.optString("detail")
            // Caso 2: lista de errores tipo FastAPI [{loc:[], msg:"...", type:"..."}, ...]
            val arr = JSONArray(raw)
            if (arr.length() > 0) {
                val msgs = (0 until arr.length()).mapNotNull { i ->
                    val item = arr.optJSONObject(i)
                    item?.optString("msg")
                }
                if (msgs.isNotEmpty()) return msgs.joinToString("\n")
            }
        } catch (_: Exception) {
            // Ignorar parseo fallido, devolvemos el raw truncado
        }
        return raw.take(500)
    }
}

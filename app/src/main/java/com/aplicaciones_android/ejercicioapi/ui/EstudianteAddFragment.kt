package com.aplicaciones_android.ejercicioapi.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.MultiAutoCompleteTextView
import android.widget.MultiAutoCompleteTextView.CommaTokenizer
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
import java.text.Normalizer

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
        val etCursos = view.findViewById<MultiAutoCompleteTextView>(R.id.etCursos)
        val btnGuardar = view.findViewById<MaterialButton>(R.id.btnGuardar)
        val progress = view.findViewById<ProgressBar>(R.id.progress)

        // Preparar adapter para auto-complete (opcional; vacío porque no consultamos la API)
        val cursosAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf())
        etCursos.setAdapter(cursosAdapter)
        etCursos.setTokenizer(CommaTokenizer())

        // No consultamos la API de cursos: permitimos texto libre o códigos.
        // Habilitar el botón inmediatamente.
        btnGuardar.isEnabled = true

        // Función local que procesa una lista de tokens (nombres o IDs) y realiza el envío
        // Construye una lista de nombres (List<String>) para enviar en `cursos_inscritos`.
        fun processAndSend(
            idValueParam: Int,
            nombreParam: String,
            apellidoParam: String,
            emailParam: String,
            githubParam: String?,
            tokensList: List<String>
        ) {
            // resolvedNames serán los nombres de cursos que se enviarán al servidor
            val resolvedNames = mutableListOf<String>()
            for (tk in tokensList) {
                val asId = tk.toIntOrNull()
                if (asId != null) {
                    // Si conocemos el nombre por ID, lo usamos; si no, enviamos el token como string
                    val name = cursosById[asId]
                    if (name != null) resolvedNames.add(name) else resolvedNames.add(tk)
                } else {
                    // Es un nombre: si tenemos catálogo y hay una forma canónica, podemos usarla
                    val norm = normalize(tk)
                    val idFromName = cursosByName[norm]
                    if (idFromName != null) {
                        // usar el nombre oficial del catálogo cuando esté disponible
                        val official = cursosById[idFromName]
                        if (!official.isNullOrEmpty()) resolvedNames.add(official) else resolvedNames.add(tk)
                    } else {
                        // No conocemos dicho curso en el catálogo (o no está cargado): enviarlo tal cual
                        resolvedNames.add(tk)
                    }
                }
            }

            if (nombreParam.isEmpty() || apellidoParam.isEmpty() || emailParam.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.msg_campos_obligatorios), Toast.LENGTH_LONG).show()
                return
            }

            val body = EstudianteCreateRequest(
                id = idValueParam,
                nombre = nombreParam,
                apellido = apellidoParam,
                email = emailParam,
                githubUrl = githubParam,
                cursosInscritos = resolvedNames
            )

            Log.d("ADD_ESTUDIANTE", "tokens=$tokensList resolvedNames=$resolvedNames")

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
            // idValue ahora es seguro (no null y > 0) — usarlo directamente
            val idValueNonNull = idValue

            val nombre = etNombre.text?.toString()?.trim().orEmpty()
            val apellido = etApellido.text?.toString()?.trim().orEmpty()
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val github = etGithub.text?.toString()?.trim()?.ifEmpty { null }

            // Cursos: aceptar IDs o nombres o texto libre separados por coma
            val cursosInput = etCursos.text?.toString()?.trim().orEmpty()
            val tokens = cursosInput.split(',').map { it.trim() }.filter { it.isNotEmpty() }

            // Procesar normalmente con todos los tokens (se enviarán como strings)
            processAndSend(idValueNonNull, nombre, apellido, email, github, tokens)
        }
    }

    // Normaliza una cadena: trim, lowercase y remover diacríticos (acentos)
    private fun normalize(input: String?): String {
        if (input == null) return ""
        val trimmed = input.trim().lowercase()
        val normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
        // Eliminar marcas de combinación (diacríticos)
        return normalized.replace("\\p{M}+".toRegex(), "")
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

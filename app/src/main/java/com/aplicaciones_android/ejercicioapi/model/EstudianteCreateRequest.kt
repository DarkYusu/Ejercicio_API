package com.aplicaciones_android.ejercicioapi.model

import com.google.gson.annotations.SerializedName

/**
 * Cuerpo para crear un estudiante en la API.
 */
data class EstudianteCreateRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("email") val email: String,
    @SerializedName("github_url") val githubUrl: String?,
    // La API recibe cursos como strings (p. ej. nombres); la DB espera un arreglo, así que enviamos lista de strings
    @SerializedName("cursos_inscritos") val cursosInscritos: List<String> = emptyList()
)

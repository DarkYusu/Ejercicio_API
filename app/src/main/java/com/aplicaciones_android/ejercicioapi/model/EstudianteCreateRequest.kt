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
    @SerializedName("cursos_inscritos") val cursosInscritos: List<String> = emptyList()
)

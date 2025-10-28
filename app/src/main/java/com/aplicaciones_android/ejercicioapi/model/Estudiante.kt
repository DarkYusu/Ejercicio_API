package com.aplicaciones_android.ejercicioapi.model

import com.google.gson.annotations.SerializedName

data class Estudiante(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("apellido") val apellido: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("github_url") val githubUrl: String?,
    // IDs de cursos cuando la API devuelve enteros
    @SerializedName("cursos_inscritos") val cursosInscritos: List<Int>?,
    // Alternativa: nombres de cursos cuando la API devuelve strings (rellenado por deserializador personalizado)
    val cursosInscritosNombres: List<String>? = null,
    @SerializedName(value = "curso", alternate = ["curso_inscrito", "cursoAsignado", "course"]) val curso: Curso? = null,
    @SerializedName(value = "cursos", alternate = ["cursos_detalle", "courses", "materias"]) val cursos: List<Curso>? = null
)

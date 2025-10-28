package com.aplicaciones_android.ejercicioapi.model

import com.google.gson.annotations.SerializedName

data class Curso(
    @SerializedName("id") val id: Int,
    @SerializedName(value = "nombre", alternate = ["name", "titulo", "nombre_curso"]) val nombre: String?,
    @SerializedName(value = "descripcion", alternate = ["description"]) val descripcion: String?
)

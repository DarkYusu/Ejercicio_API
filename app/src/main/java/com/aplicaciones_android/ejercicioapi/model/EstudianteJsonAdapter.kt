package com.aplicaciones_android.ejercicioapi.model

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Deserializador flexible para Estudiante:
 * - "cursos_inscritos" puede venir como [Int] (ids) o [String] (nombres).
 * - Soporta alias para "curso" y "cursos" según el modelo.
 */
class EstudianteJsonAdapter : JsonDeserializer<Estudiante> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Estudiante {
        val obj = json.asJsonObject

        fun JsonObject.optString(name: String): String? = if (has(name) && get(name) !is JsonNull) get(name).asString else null
        fun JsonObject.optInt(name: String): Int? = if (has(name) && get(name) !is JsonNull) runCatching { get(name).asInt }.getOrNull() else null
        fun JsonObject.optArray(name: String): JsonArray? = if (has(name) && get(name).isJsonArray) getAsJsonArray(name) else null

        val id = obj.optInt("id") ?: 0
        val nombre = obj.optString("nombre")
        val apellido = obj.optString("apellido")
        val email = obj.optString("email")
        val githubUrl = obj.optString("github_url")

        var cursosIds: List<Int>? = null
        var cursosNombres: List<String>? = null

        obj.optArray("cursos_inscritos")?.let { arr ->
            if (arr.size() > 0) {
                val first = arr[0]
                if (first.isJsonPrimitive) {
                    val prim = first.asJsonPrimitive
                    if (prim.isNumber) {
                        cursosIds = arr.mapNotNull { e -> runCatching { e.asInt }.getOrNull() }
                    } else if (prim.isString) {
                        cursosNombres = arr.map { e -> e.asString }
                    }
                }
            } else {
                cursosIds = emptyList()
            }
        }

        fun firstPresent(vararg keys: String): JsonElement? = keys.firstNotNullOfOrNull { k -> if (obj.has(k)) obj.get(k) else null }

        val cursoElem = firstPresent("curso", "curso_inscrito", "cursoAsignado", "course")
        val curso = if (cursoElem != null && cursoElem.isJsonObject) context.deserialize<Curso>(cursoElem, Curso::class.java) else null

        val cursosElem = firstPresent("cursos", "cursos_detalle", "courses", "materias")
        val cursos: List<Curso>? = if (cursosElem != null && cursosElem.isJsonArray) {
            val arr: Array<Curso> = context.deserialize(cursosElem, Array<Curso>::class.java)
            arr.toList()
        } else null

        return Estudiante(
            id = id,
            nombre = nombre,
            apellido = apellido,
            email = email,
            githubUrl = githubUrl,
            cursosInscritos = cursosIds,
            cursosInscritosNombres = cursosNombres,
            curso = curso,
            cursos = cursos
        )
    }
}

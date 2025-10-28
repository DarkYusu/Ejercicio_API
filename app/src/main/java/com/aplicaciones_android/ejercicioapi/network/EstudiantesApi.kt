package com.aplicaciones_android.ejercicioapi.network

import com.aplicaciones_android.ejercicioapi.model.Curso
import com.aplicaciones_android.ejercicioapi.model.Estudiante
import com.aplicaciones_android.ejercicioapi.model.EstudianteCreateRequest
import com.aplicaciones_android.ejercicioapi.model.EstudianteUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface EstudiantesApi {
    @GET("antonio/estudiantes")
    suspend fun getEstudiantes(): Response<List<Estudiante>>

    @GET("antonio/estudiantes/{id}")
    suspend fun getEstudiante(@Path("id") id: Int): Response<Estudiante>

    @DELETE("antonio/estudiantes/{id}")
    suspend fun deleteEstudiante(@Path("id") id: Int): Response<Void>

    @POST("antonio/estudiantes")
    suspend fun createEstudiante(@Body body: EstudianteCreateRequest): Response<Estudiante>

    @PUT("antonio/estudiantes/{id}")
    suspend fun updateEstudiante(@Path("id") id: Int, @Body body: EstudianteUpdateRequest): Response<Estudiante>

    // Cursos
    @GET("antonio/cursos")
    suspend fun getCursos(): Response<List<Curso>>

    @GET("antonio/cursos/{id}")
    suspend fun getCurso(@Path("id") id: Int): Response<Curso>
}

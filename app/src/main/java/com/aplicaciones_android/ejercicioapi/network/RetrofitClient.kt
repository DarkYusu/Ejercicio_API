package com.aplicaciones_android.ejercicioapi.network

import com.aplicaciones_android.ejercicioapi.model.Estudiante
import com.aplicaciones_android.ejercicioapi.model.EstudianteJsonAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://test-poke-341597259134.southamerica-west1.run.app/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Estudiante::class.java, EstudianteJsonAdapter())
            .create()
    }

    val api: EstudiantesApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(EstudiantesApi::class.java)
    }
}

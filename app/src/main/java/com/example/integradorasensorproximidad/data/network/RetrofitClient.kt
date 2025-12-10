package com.example.integradorasensorproximidad.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {

    private const val BASE_URL = "http://192.168.100.26:5000/"

    // Creamos un interceptor de logging para ver las peticiones y respuestas en el Logcat
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Creamos un cliente OkHttp y le añadimos el interceptor
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Creamos la instancia de Retrofit de forma perezosa (lazy)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Exponemos el servicio de la API, también creado de forma perezosa
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}

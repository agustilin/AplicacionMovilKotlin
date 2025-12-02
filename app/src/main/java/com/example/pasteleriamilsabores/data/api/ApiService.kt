package com.example.pasteleriamilsabores.data.api

import com.example.pasteleriamilsabores.data.model.ApiProduct
import retrofit2.Response
import retrofit2.http.GET

interface ProductApiService {
    @GET("chalalo1533/ServicioRest/refs/heads/master/productos.json")
    suspend fun getProducts(): Response<List<ApiProduct>>
}
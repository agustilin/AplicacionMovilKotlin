package com.example.pasteleriamilsabores.data.model

import com.google.gson.annotations.SerializedName

data class ApiProduct(
    @SerializedName("sku")
    val sku: String,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("categoria")
    val categoria: String,

    @SerializedName("descripcion")
    val descripcion: String,

    @SerializedName("precio")
    val precio: Int,

    @SerializedName("imagen")
    val imagen: String,

    @SerializedName("stock_por_sucursal")
    val stockPorSucursal: Map<String, Any>
) {

    fun getStock(sucursal: String): Int {
        return try {
            (stockPorSucursal[sucursal] as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
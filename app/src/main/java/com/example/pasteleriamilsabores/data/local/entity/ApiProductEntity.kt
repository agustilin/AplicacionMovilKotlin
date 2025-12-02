package com.example.pasteleriamilsabores.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_products")
data class ApiProductEntity(
    @PrimaryKey
    val sku: String,
    val nombre: String,
    val categoria: String,
    val descripcion: String,
    val precio: Int,
    val imagen: String,
    val stockConcepcion: Int,
    val stockTalcahuano: Int,
    val stockChillan: Int,
    val stockLosAngeles: Int
)
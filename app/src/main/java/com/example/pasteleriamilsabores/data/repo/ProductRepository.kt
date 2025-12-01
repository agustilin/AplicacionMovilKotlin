package com.example.pasteleriamilsabores.data.repo

import android.content.Context
import com.example.pasteleriamilsabores.data.api.RetrofitClient
import com.example.pasteleriamilsabores.data.local.AppDatabase
import com.example.pasteleriamilsabores.data.local.entity.ApiProductEntity
import com.example.pasteleriamilsabores.data.local.entity.ProductEntity
import com.example.pasteleriamilsabores.data.model.ApiProduct
import com.example.pasteleriamilsabores.data.model.Product
import com.example.pasteleriamilsabores.utils.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val productDao = db.productDao()
    private val apiProductDao = db.apiProductDao()
    private val networkHelper = NetworkHelper(context)
    private val apiService = RetrofitClient.productApiService

    // ========== PARA PRODUCTOS DE PASTELERÍA (EXISTENTE) ==========

    fun getProductsFlow(): Flow<List<Product>> {
        return productDao.getAllFlow().map { list ->
            list.map { entity ->
                Product(
                    id = entity.id,
                    titulo = entity.titulo,
                    descripcion = entity.descripcion,
                    precio = entity.precio,
                    imagen = entity.imagen,
                    forma = entity.forma,
                    tamanio = entity.tamanio,
                    stock = entity.stock
                )
            }
        }
    }

    suspend fun getProductById(id: Int): Product? = withContext(Dispatchers.IO) {
        productDao.getByid(id)?.let { entity ->
            Product(
                id = entity.id,
                titulo = entity.titulo,
                descripcion = entity.descripcion,
                precio = entity.precio,
                imagen = entity.imagen,
                forma = entity.forma,
                tamanio = entity.tamanio,
                stock = entity.stock
            )
        }
    }

    // ========== PARA PRODUCTOS DE LA API (NUEVO) ==========

    suspend fun loadProductsFromApi(): Result<List<ApiProduct>> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkHelper.isNetworkAvailable()) {
                Result.failure(Exception("Sin conexión a Internet"))
            } else {
                val response = apiService.getProducts()
                if (response.isSuccessful) {
                    val products = response.body() ?: emptyList()
                    Result.success(products)
                } else {
                    Result.failure(Exception("Error del servidor: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error al cargar productos: ${e.message}"))
        }
    }

    suspend fun saveApiProductsToDatabase(apiProducts: List<ApiProduct>): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val entities = apiProducts.map { apiProduct ->
                ApiProductEntity(
                    sku = apiProduct.sku,
                    nombre = apiProduct.nombre,
                    categoria = apiProduct.categoria,
                    descripcion = apiProduct.descripcion,
                    precio = apiProduct.precio,
                    imagen = apiProduct.imagen,
                    stockConcepcion = apiProduct.getStock("Concepción"),
                    stockTalcahuano = apiProduct.getStock("Talcahuano"),
                    stockChillan = apiProduct.getStock("Chillán"),
                    stockLosAngeles = apiProduct.getStock("Los Ángeles")
                )
            }
            apiProductDao.insertAll(entities)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Error al guardar productos: ${e.message}"))
        }
    }

    suspend fun getApiProductsFromDatabase(): Result<List<ApiProduct>> = withContext(Dispatchers.IO) {
        return@withContext try {

            val productEntities = apiProductDao.getAllFlow().first()

            val apiProducts = productEntities.map { entity ->
                ApiProduct(
                    sku = entity.sku,
                    nombre = entity.nombre,
                    categoria = entity.categoria,
                    descripcion = entity.descripcion,
                    precio = entity.precio,
                    imagen = entity.imagen,
                    stockPorSucursal = mapOf(
                        "Concepción" to entity.stockConcepcion,
                        "Talcahuano" to entity.stockTalcahuano,
                        "Chillán" to entity.stockChillan,
                        "Los Ángeles" to entity.stockLosAngeles
                    )
                )
            }
            Result.success(apiProducts)
        } catch (e: Exception) {
            Result.failure(Exception("Error al cargar productos locales: ${e.message}"))
        }
    }

    suspend fun hasApiProductsInDatabase(): Boolean = withContext(Dispatchers.IO) {
        return@withContext apiProductDao.count() > 0
    }

    suspend fun clearApiProducts(): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            apiProductDao.clearAll()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Error al limpiar productos: ${e.message}"))
        }
    }

    fun isOnline(): Boolean {
        return networkHelper.isNetworkAvailable()
    }

   //para pasteleria
    suspend fun initializeIfNeeded(forceUpdate: Boolean = false) {

    }
}
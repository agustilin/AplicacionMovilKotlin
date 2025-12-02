package com.example.pasteleriamilsabores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pasteleriamilsabores.data.model.ApiProduct
import com.example.pasteleriamilsabores.data.repo.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProductManagerState {
    object Loading : ProductManagerState()
    data class Success(val products: List<ApiProduct>) : ProductManagerState()
    data class Error(val message: String) : ProductManagerState()
    object Empty : ProductManagerState()
}

class ProductManagerViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductManagerState>(ProductManagerState.Empty)
    val uiState: StateFlow<ProductManagerState> = _uiState.asStateFlow()

    private val _currentProducts = MutableStateFlow<List<ApiProduct>>(emptyList())
    val currentProducts: StateFlow<List<ApiProduct>> = _currentProducts.asStateFlow()

    private val _hasLocalData = MutableStateFlow(false)
    val hasLocalData: StateFlow<Boolean> = _hasLocalData.asStateFlow()

    init {
        // Verificar datos locales al inicializar
        checkLocalData()
    }

    // Cargar desde API REST
    fun loadFromApi() {
        viewModelScope.launch {
            _uiState.value = ProductManagerState.Loading

            val result = productRepository.loadProductsFromApi()
            if (result.isSuccess) {
                val products = result.getOrNull() ?: emptyList()
                _currentProducts.value = products
                _uiState.value = if (products.isEmpty()) {
                    ProductManagerState.Empty
                } else {
                    ProductManagerState.Success(products)
                }
            } else {
                _uiState.value = ProductManagerState.Error(
                    result.exceptionOrNull()?.message ?: "Error desconocido al cargar desde API"
                )
            }
        }
    }

    // Cargar desde Base de Datos Local
    fun loadFromDatabase() {
        viewModelScope.launch {
            _uiState.value = ProductManagerState.Loading

            val result = productRepository.getApiProductsFromDatabase()
            if (result.isSuccess) {
                val products = result.getOrNull() ?: emptyList()
                _currentProducts.value = products
                _uiState.value = if (products.isEmpty()) {
                    ProductManagerState.Empty
                } else {
                    ProductManagerState.Success(products)
                }
            } else {
                _uiState.value = ProductManagerState.Error(
                    result.exceptionOrNull()?.message ?: "Error al cargar datos locales"
                )
            }
        }
    }

    // Guardar productos actuales en BD local
    fun saveToDatabase() {
        viewModelScope.launch {
            val currentProducts = _currentProducts.value
            if (currentProducts.isNotEmpty()) {
                val result = productRepository.saveApiProductsToDatabase(currentProducts)
                if (result.isSuccess) {
                    // Actualizar estado de datos locales después de guardar
                    checkLocalData()
                }
            }
        }
    }

    // Verificar si hay conexión a internet
    fun isOnline(): Boolean {
        return productRepository.isOnline()
    }

    // Verificar datos locales
    private fun checkLocalData() {
        viewModelScope.launch {
            _hasLocalData.value = productRepository.hasApiProductsInDatabase()
        }
    }

    // Factory para el ViewModel
    companion object {
        fun factory(productRepository: ProductRepository): androidx.lifecycle.ViewModelProvider.Factory {
            return object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProductManagerViewModel(productRepository) as T
                }
            }
        }
    }
}
package com.example.pasteleriamilsabores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pasteleriamilsabores.data.repo.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    data class Success(val filteredProducts: List<com.example.pasteleriamilsabores.data.model.Product>) : UiState()
    sealed class Error : UiState() {
        data class DatabaseError(val message: String) : Error()
        data class AssetError(val message: String) : Error()
        data class JsonError(val message: String) : Error()
        data class UnknownError(val message: String) : Error()
    }
}

class ProductViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val allProducts = mutableListOf<com.example.pasteleriamilsabores.data.model.Product>()

    init {
        loadProducts()
    }

    fun loadProducts(forceUpdate: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                productRepository.initializeIfNeeded(forceUpdate)
                val productsFlow = productRepository.getProductsFlow()

                productsFlow.collect { products ->
                    allProducts.clear()
                    allProducts.addAll(products)
                    filterProducts()
                }
            } catch (e: Exception) {
                //Manejo de errores
                val errorMessage = when {
                    e.message?.contains("base de datos") == true ->
                        "Error al acceder a la base de datos"
                    e.message?.contains("cargar") == true ->
                        "Error al cargar los datos iniciales"
                    e.message?.contains("JSON") == true || e.message?.contains("parse") == true ->
                        "Error al procesar los datos"
                    else -> e.message ?: "Error desconocido"
                }

                _uiState.value = UiState.Error.UnknownError(errorMessage)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterProducts()
    }

    private fun filterProducts() {
        val query = _searchQuery.value
        val filtered = if (query.isBlank()) {
            allProducts
        } else {
            allProducts.filter { product ->
                product.titulo.contains(query, ignoreCase = true) ||
                        product.descripcion.contains(query, ignoreCase = true)
            }
        }

        _uiState.update { currentState ->
            when (currentState) {
                is UiState.Success -> UiState.Success(filtered)
                else -> UiState.Success(filtered)
            }
        }
    }

    suspend fun getProductById(id: Int): com.example.pasteleriamilsabores.data.model.Product? {
        return productRepository.getProductById(id)
    }

    companion object {
        fun factory(productRepository: ProductRepository): androidx.lifecycle.ViewModelProvider.Factory {
            return object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProductViewModel(productRepository) as T
                }
            }
        }
    }
}
package com.example.pasteleriamilsabores.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pasteleriamilsabores.data.model.ApiProduct

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiProductDetailScreen(
    product: ApiProduct,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Detalle del Producto",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Imagen del producto
            if (product.imagen.isNotBlank() && product.imagen != "https://example.com/img/PROD-001-ELE.jpg") {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imagen)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Imagen de ${product.nombre}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Información del producto
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // SKU
                DetailItem(
                    label = "SKU",
                    value = product.sku
                )

                // Nombre
                DetailItem(
                    label = "Nombre",
                    value = product.nombre,
                    isTitle = true
                )

                // Categoría
                DetailItem(
                    label = "Categoría",
                    value = product.categoria
                )

                // Precio
                DetailItem(
                    label = "Precio",
                    value = "$${product.precio}",
                    isHighlighted = true
                )

                // Descripción
                DetailItem(
                    label = "Descripción",
                    value = product.descripcion,
                    isMultiline = true
                )

                // Stock por Sucursal
                Text(
                    text = "Stock por Sucursal:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                product.stockPorSucursal.forEach { (sucursal, stock) ->
                    StockItem(
                        sucursal = sucursal,
                        stock = stock
                    )
                }

                // Stock Total
                val totalStock = calculateTotalStock(product.stockPorSucursal)
                DetailItem(
                    label = "Stock Total",
                    value = totalStock.toString(),
                    isHighlighted = totalStock > 0
                )
            }
        }
    }
}

// calcular stock total
private fun calculateTotalStock(stockPorSucursal: Map<String, Any>): Int {
    return stockPorSucursal.values.sumOf { stockValue ->
        try {
            (stockValue as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    isTitle: Boolean = false,
    isHighlighted: Boolean = false,
    isMultiline: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = if (isTitle) MaterialTheme.typography.headlineSmall
                else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isTitle || isHighlighted) FontWeight.Bold else FontWeight.Normal,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                modifier = if (isMultiline) Modifier.fillMaxWidth() else Modifier
            )
        }
    }
}

@Composable
fun StockItem(
    sucursal: String,
    stock: Any
) {
    // Convertir stock a Int
    val stockInt = try {
        (stock as? Number)?.toInt() ?: 0
    } catch (e: Exception) {
        0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sucursal,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (stockInt > 0) "$stockInt unidades" else "Sin stock",
                style = MaterialTheme.typography.bodyMedium,
                color = if (stockInt > 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
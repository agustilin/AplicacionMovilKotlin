package com.example.pasteleriamilsabores.data.local.dao

import androidx.room.*
import com.example.pasteleriamilsabores.data.local.entity.ApiProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiProductDao {
    @Query("SELECT * FROM api_products")
    fun getAllFlow(): Flow<List<ApiProductEntity>>

    @Query("SELECT * FROM api_products WHERE sku = :sku LIMIT 1")
    suspend fun getBySku(sku: String): ApiProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ApiProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ApiProductEntity)

    @Query("DELETE FROM api_products")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM api_products")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM api_products LIMIT 1)")
    suspend fun hasData(): Boolean
}
package com.vodileats.core.database.dao

import androidx.room.*
import com.vodileats.core.database.entity.CartItemEntity
import com.vodileats.core.database.entity.CachedRestaurantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items")
    fun getAllCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE menuItemId = :menuItemId")
    suspend fun getCartItem(menuItemId: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateItem(item: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE menuItemId = :menuItemId")
    suspend fun updateQuantity(menuItemId: String, quantity: Int)

    @Delete
    suspend fun deleteItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE menuItemId = :menuItemId")
    suspend fun deleteItemById(menuItemId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    @Query("SELECT COUNT(*) FROM cart_items")
    fun getCartCount(): Flow<Int>
}

@Dao
interface RestaurantDao {
    @Query("SELECT * FROM cached_restaurants ORDER BY rating DESC")
    fun getAllCachedRestaurants(): Flow<List<CachedRestaurantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(restaurants: List<CachedRestaurantEntity>)

    @Query("DELETE FROM cached_restaurants")
    suspend fun clearAll()
}

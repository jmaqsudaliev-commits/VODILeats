package com.vodileats.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vodileats.core.database.dao.CartDao
import com.vodileats.core.database.dao.RestaurantDao
import com.vodileats.core.database.entity.CartItemEntity
import com.vodileats.core.database.entity.CachedRestaurantEntity

@Database(
    entities = [
        CartItemEntity::class,
        CachedRestaurantEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VodilEatsDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun restaurantDao(): RestaurantDao
}

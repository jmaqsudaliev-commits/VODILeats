package com.vodileats.core.database.di

import android.content.Context
import androidx.room.Room
import com.vodileats.core.database.VodilEatsDatabase
import com.vodileats.core.database.dao.CartDao
import com.vodileats.core.database.dao.RestaurantDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVodilEatsDatabase(
        @ApplicationContext context: Context
    ): VodilEatsDatabase {
        return Room.databaseBuilder(
            context,
            VodilEatsDatabase::class.java,
            "vodil_eats.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideCartDao(database: VodilEatsDatabase): CartDao {
        return database.cartDao()
    }

    @Provides
    @Singleton
    fun provideRestaurantDao(database: VodilEatsDatabase): RestaurantDao {
        return database.restaurantDao()
    }
}

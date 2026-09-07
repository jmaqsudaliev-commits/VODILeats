package com.vodileats.core.network.di

import com.vodileats.core.domain.repository.AuthRepository
import com.vodileats.core.domain.repository.CourierRepository
import com.vodileats.core.domain.repository.OrderRepository
import com.vodileats.core.domain.repository.RestaurantRepository
import com.vodileats.core.network.repository.AuthRepositoryImpl
import com.vodileats.core.network.repository.CourierRepositoryImpl
import com.vodileats.core.network.repository.OrderRepositoryImpl
import com.vodileats.core.network.repository.RestaurantRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindRestaurantRepository(
        restaurantRepositoryImpl: RestaurantRepositoryImpl
    ): RestaurantRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        orderRepositoryImpl: OrderRepositoryImpl
    ): OrderRepository

    @Binds
    @Singleton
    abstract fun bindCourierRepository(
        courierRepositoryImpl: CourierRepositoryImpl
    ): CourierRepository
}

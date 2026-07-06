package com.filmatube.app.di

import com.filmatube.app.data.auth.AuthRepositoryImpl
import com.filmatube.app.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds repository interfaces (domain) to their implementations (data).
 * More `@Binds` entries are added as each repository is built (movies, watch progress, social…).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}

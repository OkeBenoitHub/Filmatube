package com.filmatube.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds repository interfaces (domain) to their implementations (data).
 *
 * `@Binds` entries are added as each repository is built (auth, movies, watch progress, social…).
 * Abstract + installed now so the DI structure is in place.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule

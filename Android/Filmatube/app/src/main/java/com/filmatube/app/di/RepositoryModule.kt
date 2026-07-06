package com.filmatube.app.di

import com.filmatube.app.data.auth.AuthRepositoryImpl
import com.filmatube.app.data.movie.MovieRepositoryImpl
import com.filmatube.app.data.user.ProfilesRepositoryImpl
import com.filmatube.app.data.user.UserRepositoryImpl
import com.filmatube.app.domain.repository.AuthRepository
import com.filmatube.app.domain.repository.MovieRepository
import com.filmatube.app.domain.repository.ProfilesRepository
import com.filmatube.app.domain.repository.UserRepository
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

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindProfilesRepository(impl: ProfilesRepositoryImpl): ProfilesRepository

    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository
}

package com.filmatube.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Networking dependencies (OkHttp, Retrofit, JSON converter, API services for TMDB and the
 * R2 presign/stream endpoints).
 *
 * Providers are added on **Day 5** (Retrofit/OkHttp setup). Kept as an empty, installed module
 * for now so the DI structure is in place.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule

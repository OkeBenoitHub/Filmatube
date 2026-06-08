package com.filmatube.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Firebase dependencies (Auth, Firestore, Messaging, App Check).
 *
 * Providers are added on **Day 5**, once `google-services.json` and Firebase initialization are in
 * place. Kept as an empty, installed module for now so the DI structure is in place.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule

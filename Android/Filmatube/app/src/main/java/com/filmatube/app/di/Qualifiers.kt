package com.filmatube.app.di

import javax.inject.Qualifier

/** Marks the IO-bound coroutine dispatcher (network, disk, Firebase). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Marks the default (CPU-bound) coroutine dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** Marks the main/UI coroutine dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

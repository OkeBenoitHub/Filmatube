package com.filmatube.app.di

import android.content.Context
import androidx.room.Room
import com.filmatube.app.data.download.DownloadedMovieDao
import com.filmatube.app.data.local.FilmatubeDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): FilmatubeDatabase =
        Room.databaseBuilder(context, FilmatubeDatabase::class.java, "filmatube.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDownloadedMovieDao(database: FilmatubeDatabase): DownloadedMovieDao =
        database.downloadedMovieDao()
}

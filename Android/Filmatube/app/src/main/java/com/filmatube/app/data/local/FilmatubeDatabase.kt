package com.filmatube.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.filmatube.app.data.download.DownloadedMovie
import com.filmatube.app.data.download.DownloadedMovieDao

@Database(entities = [DownloadedMovie::class], version = 2, exportSchema = false)
abstract class FilmatubeDatabase : RoomDatabase() {
    abstract fun downloadedMovieDao(): DownloadedMovieDao
}

package com.filmatube.app.data.download

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Locally-cached display metadata for a downloaded movie (survives offline). */
@Entity(tableName = "downloaded_movies")
data class DownloadedMovie(
    @PrimaryKey val movieId: String,
    val titleEn: String,
    val titleFr: String,
    val posterUrl: String,
    val backdropUrl: String,
    val durationMin: Int,
    val subtitlesJson: String, // JSON array of {lang,url}
    val addedAt: Long,
    val expiresAt: Long, // license window end (epoch ms)
)

@Dao
interface DownloadedMovieDao {
    @Query("SELECT * FROM downloaded_movies ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<DownloadedMovie>>

    @Query("SELECT * FROM downloaded_movies WHERE movieId = :movieId")
    suspend fun get(movieId: String): DownloadedMovie?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(movie: DownloadedMovie)

    @Query("DELETE FROM downloaded_movies WHERE movieId = :movieId")
    suspend fun delete(movieId: String)

    @Query("DELETE FROM downloaded_movies")
    suspend fun deleteAll()
}

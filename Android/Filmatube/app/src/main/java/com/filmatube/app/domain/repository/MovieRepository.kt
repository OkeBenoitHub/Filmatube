package com.filmatube.app.domain.repository

import com.filmatube.app.domain.model.Movie

enum class MovieSort { NEWEST, RATING, ALPHA }

/** Read access to the published movie catalog. */
interface MovieRepository {
    suspend fun getFeatured(limit: Int = 10): List<Movie>
    suspend fun getTrending(limit: Int = 15): List<Movie>
    suspend fun getNewReleases(limit: Int = 15): List<Movie>
    suspend fun getComingSoon(limit: Int = 15): List<Movie>
    suspend fun getByGenre(genre: String, limit: Int = 15): List<Movie>
    suspend fun getMovie(id: String): Movie?

    /** Movies sharing a genre with the given one (excludes it). */
    suspend fun getRelated(movieId: String, genres: List<String>, limit: Int = 15): List<Movie>

    /** Grid browse with client-side genre/year filtering + sort. */
    suspend fun browse(
        sort: MovieSort = MovieSort.NEWEST,
        genre: String? = null,
        year: Int? = null,
        limit: Int = 40,
    ): List<Movie>

    /** Title search (client-side contains match over the published catalog). */
    suspend fun search(query: String, limit: Int = 40): List<Movie>
}

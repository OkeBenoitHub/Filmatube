package com.filmatube.app.domain.repository

import com.filmatube.app.domain.model.Movie

/**
 * Browse orderings. [POPULAR] is what the Home "Trending" row is sorted by — without it,
 * "See all" under Trending had nowhere to land that actually looked like trending.
 */
enum class MovieSort { NEWEST, POPULAR, RATING, ALPHA }

/** Read access to the published movie catalog. */
interface MovieRepository {
    suspend fun getFeatured(limit: Int = 10): List<Movie>
    suspend fun getTrending(limit: Int = 15): List<Movie>
    suspend fun getNewReleases(limit: Int = 15): List<Movie>
    suspend fun getComingSoon(limit: Int = 15): List<Movie>
    suspend fun getByGenre(genre: String, limit: Int = 15): List<Movie>
    suspend fun getMovie(id: String): Movie?

    /**
     * Fetch several movies by id, preserving the given order.
     *
     * The recommendation rows come back as an *ordered* list of ids (best match first), so the
     * result must keep that order rather than whatever Firestore returns.
     */
    suspend fun getMoviesByIds(ids: List<String>): List<Movie>

    /** Movies sharing a genre with the given one (excludes it). */
    suspend fun getRelated(movieId: String, genres: List<String>, limit: Int = 15): List<Movie>

    /**
     * Grid browse with client-side genre/year filtering + sort.
     *
     * [comingSoon] null shows everything, true narrows to unreleased titles — the "See all"
     * target for Home's Coming Soon row.
     */
    suspend fun browse(
        sort: MovieSort = MovieSort.NEWEST,
        genre: String? = null,
        year: Int? = null,
        comingSoon: Boolean? = null,
        limit: Int = 40,
    ): List<Movie>

    /** Title search (client-side contains match over the published catalog). */
    suspend fun search(query: String, limit: Int = 40): List<Movie>

    /** Movies whose cast includes the given actor name. */
    suspend fun getByActor(actorName: String, limit: Int = 40): List<Movie>
}

package com.filmatube.app.data.movie

import com.filmatube.app.domain.model.CastMember
import com.filmatube.app.domain.model.LocalizedText
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.repository.MovieRepository
import com.filmatube.app.domain.repository.MovieSort
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : MovieRepository {

    private val movies get() = firestore.collection("movies")

    private fun published() = movies.whereEqualTo("status", "published")

    override suspend fun getFeatured(limit: Int): List<Movie> =
        published()
            .whereEqualTo("isFeatured", true)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get().await().toMovies()

    override suspend fun getTrending(limit: Int): List<Movie> =
        published()
            .orderBy("viewsCount", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get().await().toMovies()

    override suspend fun getNewReleases(limit: Int): List<Movie> =
        published()
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get().await().toMovies()

    override suspend fun getComingSoon(limit: Int): List<Movie> =
        published()
            .whereEqualTo("isComingSoon", true)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get().await().toMovies()

    override suspend fun getByGenre(genre: String, limit: Int): List<Movie> =
        published()
            .whereArrayContains("genres", genre)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get().await().toMovies()

    override suspend fun getMovie(id: String): Movie? =
        movies.document(id).get().await().toMovie()

    override suspend fun getRelated(movieId: String, genres: List<String>, limit: Int): List<Movie> {
        val genre = genres.firstOrNull() ?: return emptyList()
        return getByGenre(genre, limit + 1).filter { it.id != movieId }.take(limit)
    }

    override suspend fun browse(sort: MovieSort, genre: String?, year: Int?, limit: Int): List<Movie> {
        // Index-safe: one server-side order, then filter/sort client-side.
        val ordered = when (sort) {
            MovieSort.RATING -> published().orderBy("averageRating", Query.Direction.DESCENDING)
            else -> published().orderBy("addedAt", Query.Direction.DESCENDING)
        }
        var result = ordered.limit(200).get().await().toMovies()
        if (genre != null) result = result.filter { genre in it.genres }
        if (year != null) result = result.filter { it.year == year }
        if (sort == MovieSort.ALPHA) result = result.sortedBy { it.title.en.lowercase() }
        return result.take(limit)
    }

    override suspend fun search(query: String, limit: Int): List<Movie> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()
        val all = published()
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .limit(200)
            .get().await().toMovies()
        return all.filter {
            it.title.en.lowercase().contains(q) || it.title.fr.lowercase().contains(q)
        }.take(limit)
    }

    override suspend fun getByActor(actorName: String, limit: Int): List<Movie> {
        val all = published()
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .limit(200)
            .get().await().toMovies()
        return all.filter { movie ->
            movie.cast.any { it.name.equals(actorName, ignoreCase = true) }
        }.take(limit)
    }

    private fun com.google.firebase.firestore.QuerySnapshot.toMovies(): List<Movie> =
        documents.mapNotNull { it.toMovie() }

    private fun DocumentSnapshot.toMovie(): Movie? {
        if (!exists()) return null
        val titleMap = get("title") as? Map<*, *>
        val descMap = get("description") as? Map<*, *>
        return Movie(
            id = id,
            title = LocalizedText(
                en = titleMap?.get("en") as? String ?: "",
                fr = titleMap?.get("fr") as? String ?: "",
            ),
            description = LocalizedText(
                en = descMap?.get("en") as? String ?: "",
                fr = descMap?.get("fr") as? String ?: "",
            ),
            posterUrl = getString("posterUrl").orEmpty(),
            backdropUrl = getString("backdropUrl").orEmpty(),
            trailerUrl = getString("trailerUrl"),
            genres = (get("genres") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            year = (getLong("year") ?: 0L).toInt(),
            duration = (getLong("duration") ?: 0L).toInt(),
            ageRating = getString("ageRating").orEmpty(),
            cast = (get("cast") as? List<*>)?.mapNotNull { item ->
                (item as? Map<*, *>)?.let {
                    CastMember(
                        name = it["name"] as? String ?: "",
                        character = it["character"] as? String ?: "",
                        photoUrl = it["photoUrl"] as? String ?: "",
                    )
                }
            } ?: emptyList(),
            directors = (get("directors") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            averageRating = getDouble("averageRating") ?: 0.0,
            ratingsCount = getLong("ratingsCount") ?: 0L,
            likesCount = getLong("likesCount") ?: 0L,
            viewsCount = getLong("viewsCount") ?: 0L,
            isFeatured = getBoolean("isFeatured") ?: false,
            isComingSoon = getBoolean("isComingSoon") ?: false,
        )
    }
}

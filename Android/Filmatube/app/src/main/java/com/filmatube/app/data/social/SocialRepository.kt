package com.filmatube.app.data.social

import com.filmatube.app.di.IoDispatcher
import com.filmatube.app.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** A user suggested to follow, with their taste-overlap score. */
data class SuggestedUser(
    val uid: String,
    val displayName: String,
    val avatarUrl: String,
    val tasteMatch: Int,
)

/** Taste-match % + follow suggestions from genre overlap. */
@Singleton
class SocialRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    @Volatile
    private var cachedGenres: Set<String>? = null

    private suspend fun myGenres(): Set<String> {
        cachedGenres?.let { return it }
        val uid = auth.currentUser?.uid ?: return emptySet()
        val genres = userRepository.getUser(uid)?.genrePreferences?.toSet() ?: emptySet()
        cachedGenres = genres
        return genres
    }

    /** Jaccard overlap of the current user's taste with [theirGenres], as a percentage. */
    suspend fun tasteMatch(theirGenres: List<String>): Int = tasteMatch(myGenres(), theirGenres.toSet())

    private fun tasteMatch(mine: Set<String>, theirs: Set<String>): Int =
        TasteMatch.jaccardPercent(mine, theirs)

    /** People to follow, ranked by taste overlap (excludes self + already-followed). */
    suspend fun suggestedUsers(limit: Int = 20): List<SuggestedUser> = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext emptyList()
        val mine = myGenres()
        val following = followRepository.observeFollowingIds(uid).first().toSet()

        firestore.collection("users").limit(60).get().await().documents
            .mapNotNull { d ->
                if (d.id == uid || d.id in following) return@mapNotNull null
                val genres = (d.get("genrePreferences") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                SuggestedUser(
                    uid = d.id,
                    displayName = d.getString("displayName") ?: "",
                    avatarUrl = d.getString("avatarUrl") ?: "",
                    tasteMatch = tasteMatch(mine, genres.toSet()),
                )
            }
            .filter { it.tasteMatch > 0 }
            .sortedByDescending { it.tasteMatch }
            .take(limit)
    }
}

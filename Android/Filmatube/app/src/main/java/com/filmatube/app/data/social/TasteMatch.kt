package com.filmatube.app.data.social

/**
 * Pure taste-match logic (no framework deps) so it can be unit-tested directly.
 * Jaccard similarity of two genre sets, expressed as a 0–100 percentage.
 */
object TasteMatch {
    fun jaccardPercent(mine: Set<String>, theirs: Set<String>): Int {
        if (mine.isEmpty() || theirs.isEmpty()) return 0
        val intersection = mine.intersect(theirs).size
        val union = mine.union(theirs).size
        return if (union == 0) 0 else intersection * 100 / union
    }
}

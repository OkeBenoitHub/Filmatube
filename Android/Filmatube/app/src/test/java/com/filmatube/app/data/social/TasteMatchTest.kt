package com.filmatube.app.data.social

import org.junit.Assert.assertEquals
import org.junit.Test

class TasteMatchTest {

    @Test
    fun identicalSets_are100Percent() {
        val g = setOf("action", "comedy", "scifi")
        assertEquals(100, TasteMatch.jaccardPercent(g, g))
    }

    @Test
    fun disjointSets_areZero() {
        assertEquals(0, TasteMatch.jaccardPercent(setOf("action"), setOf("drama")))
    }

    @Test
    fun partialOverlap_isJaccardPercent() {
        // intersection {action} = 1, union {action, comedy, drama} = 3 -> 33%
        val result = TasteMatch.jaccardPercent(setOf("action", "comedy"), setOf("action", "drama"))
        assertEquals(33, result)
    }

    @Test
    fun halfOverlap_isFiftyPercent() {
        // intersection {a, b} = 2, union {a, b, c, d} = 4 -> 50%
        assertEquals(50, TasteMatch.jaccardPercent(setOf("a", "b"), setOf("a", "b", "c", "d")))
    }

    @Test
    fun emptyEitherSide_isZero() {
        assertEquals(0, TasteMatch.jaccardPercent(emptySet(), setOf("action")))
        assertEquals(0, TasteMatch.jaccardPercent(setOf("action"), emptySet()))
        assertEquals(0, TasteMatch.jaccardPercent(emptySet(), emptySet()))
    }
}

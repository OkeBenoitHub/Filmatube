package com.filmatube.app.ui.browse

import com.filmatube.app.ui.taste.Genre
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The genre row is "All" followed by every [Genre], and arriving from a Home "See all" scrolls
 * to the incoming genre's chip. These cover the off-by-one for *every* genre rather than
 * whichever one happened to be on screen during manual testing.
 */
class GenreChipIndexTest {

    @Test
    fun `every genre maps to its chip, offset by the leading All chip`() {
        Genre.entries.forEachIndexed { ordinal, genre ->
            assertEquals(
                "chip index for ${genre.key}",
                ordinal + 1,
                genreChipIndex(genre.key),
            )
        }
    }

    @Test
    fun `first and last genres land on real chips`() {
        // Guards the two ends specifically: index 1 must not collide with the "All" chip at
        // 0, and the last genre must not run past the end of the row.
        assertEquals(1, genreChipIndex(Genre.entries.first().key))
        assertEquals(Genre.entries.size, genreChipIndex(Genre.entries.last().key))
    }

    @Test
    fun `no genre yields no chip to scroll to`() {
        assertEquals(-1, genreChipIndex(null))
    }

    @Test
    fun `an unknown genre key yields no chip rather than pointing at All`() {
        // A stored genrePreference can outlive its enum entry; returning 0 here would scroll
        // to "All" and imply the wrong filter is selected.
        assertEquals(-1, genreChipIndex("nonexistent-genre"))
    }
}

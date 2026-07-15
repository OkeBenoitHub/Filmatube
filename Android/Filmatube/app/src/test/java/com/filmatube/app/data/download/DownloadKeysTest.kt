package com.filmatube.app.data.download

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadKeysTest {

    @Test
    fun stripsExpiringSignatureQuery() {
        val a = "https://videos.r2.example.com/movies/abc/master.mp4?X-Amz-Signature=OLD&X-Amz-Expires=3600"
        val b = "https://videos.r2.example.com/movies/abc/master.mp4?X-Amz-Signature=NEW&X-Amz-Expires=3600"
        // Different signed URLs for the same object must map to the same cache key.
        assertEquals(DownloadKeys.stableCacheKey(a), DownloadKeys.stableCacheKey(b))
        assertEquals("https://videos.r2.example.com/movies/abc/master.mp4", DownloadKeys.stableCacheKey(a))
    }

    @Test
    fun urlWithoutQuery_isUnchanged() {
        val url = "https://videos.r2.example.com/movies/xyz/master.mp4"
        assertEquals(url, DownloadKeys.stableCacheKey(url))
    }

    @Test
    fun differentObjects_haveDifferentKeys() {
        val a = "https://cdn.example.com/a.mp4?sig=1"
        val b = "https://cdn.example.com/b.mp4?sig=1"
        assertEquals(false, DownloadKeys.stableCacheKey(a) == DownloadKeys.stableCacheKey(b))
    }
}

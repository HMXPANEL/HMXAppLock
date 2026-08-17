package com.hmx.shield.core.security

import com.hmx.shield.core.model.RelockPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockedAppCacheTest {

    @Test
    fun addAndCheckLocked() {
        val cache = LockedAppCache()
        assertFalse(cache.isLocked("com.example.app"))
        cache.add(AppLockInfo("com.example.app", "Example", RelockPolicy.INSTANT))
        assertTrue(cache.isLocked("com.example.app"))
    }

    @Test
    fun removeDropsLock() {
        val cache = LockedAppCache()
        cache.add(AppLockInfo("com.example.app", "Example", RelockPolicy.INSTANT))
        cache.remove("com.example.app")
        assertFalse(cache.isLocked("com.example.app"))
    }

    @Test
    fun replaceAllRebuildsCache() {
        val cache = LockedAppCache()
        cache.add(AppLockInfo("old", "Old", RelockPolicy.INSTANT))
        cache.replaceAll(listOf(AppLockInfo("new", "New", RelockPolicy.INSTANT)))
        assertFalse(cache.isLocked("old"))
        assertTrue(cache.isLocked("new"))
        assertEquals(1, cache.snapshot().size)
    }
}

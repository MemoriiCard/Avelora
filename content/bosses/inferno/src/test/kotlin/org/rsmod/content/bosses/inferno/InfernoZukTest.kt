package org.rsmod.content.bosses.inferno

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

class InfernoZukTest {
    @Test
    fun `glyph sweeps the arena and turns at both edges`() {
        val shield = ZukShield(startTick = 0)
        val span = ZukArena.GLYPH_MAX_X - ZukArena.GLYPH_MIN_X
        val xs = (0..span * 2 * ZukArena.GLYPH_STEP_TICKS).map { shield.x(it) }
        assertEquals(ZukArena.GLYPH_MIN_X, xs.first())
        assertEquals(ZukArena.GLYPH_MAX_X, xs.max())
        assertEquals(ZukArena.GLYPH_MIN_X, xs.last())
        assertTrue(xs.zipWithNext().all { (a, b) -> kotlin.math.abs(a - b) <= 1 })
    }

    @Test
    fun `shield only covers players behind it`() {
        val shield = ZukShield(startTick = 0)
        val x = shield.x(0)
        assertTrue(shield.covers(x, ZukArena.GLYPH_Z - 3, 0))
        assertFalse(shield.covers(x + ZukArena.HALF_WIDTH + 1, ZukArena.GLYPH_Z - 3, 0))
        assertFalse(shield.covers(x, ZukArena.GLYPH_Z + 3, 0))
    }

    @Test
    fun `shield breaks after taking its hitpoints in damage`() {
        val shield = ZukShield(startTick = 0)
        shield.absorb(ZukStats.SHIELD_HP - 1)
        assertFalse(shield.broken)
        shield.absorb(5)
        assertTrue(shield.broken)
        assertEquals(0, shield.hp)
        assertFalse(shield.covers(shield.x(0), ZukArena.GLYPH_Z - 3, 0))
    }

    @Test
    fun `instance offset moves the shield with the map copy`() {
        val shield = ZukShield(startTick = 0, dx = 100, dz = 200)
        val glyph = shield.glyphCoords(0)
        assertTrue(shield.covers(glyph.x + ZukArena.HALF_WIDTH, ZukArena.GLYPH_Z + 200 - 3, 0))
        assertEquals(ZukArena.GLYPH_Z + 200, glyph.z)
    }

    @Test
    fun `zuk specs are registered`() {
        val covered = infernoSpecs().flatMap { it.npcTypes }
        for (npc in listOf(ZukStats.NPC, ZukStats.HEALER, ZukStats.JAD_HEALER)) assertTrue(npc in covered, npc)
        for (npc in listOf(ZukStats.NPC, ZukStats.HEALER, ZukStats.JAD_HEALER)) {
            assertTrue(npc in InfernoMonsters.DEATHS, npc)
        }
    }

    @Test
    fun `zuk wave follows the final wave`() {
        assertEquals(InfernoWaves.FINAL_WAVE + 1, InfernoWaves.ZUK_WAVE)
    }

    @ResourceLock("server-cache")
    @Test
    fun `zuk symbols resolve and hitpoints match the wiki`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (npc in listOf(ZukStats.NPC, ZukStats.GLYPH, ZukStats.HEALER, ZukStats.JAD_HEALER)) {
                assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
            }
            for (obj in listOf(ZukStats.CAPE, ZukStats.PET)) {
                assertNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)), obj)
            }
            val zuk = ServerCacheManager.getNpc(ZukStats.NPC.asRSCM(RSCMType.NPC))
            assertEquals(1200, zuk?.hitpoints)
        } finally {
            cache.close()
        }
    }
}

package org.rsmod.content.travel.fairyrings

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.FairyringRow

class FairyRingCodeTest {
    @Test
    fun `dials map to the first letter of each ring`() {
        assertEquals("AIP", FairyRingCode.of(0, 0, 0))
        assertEquals("ALS", FairyRingCode.of(0, 1, 1))
        assertEquals("BJQ", FairyRingCode.of(3, 3, 3))
    }

    @Test
    fun `dial values wrap around`() {
        assertEquals(FairyRingCode.of(0, 0, 0), FairyRingCode.of(4, 4, 4))
    }
}

@ResourceLock("server-cache")
class FairyRingTableTest {
    @Test
    fun `every dial combination has a destination row`() {
        val cache = ServerCacheManager.init(240)
        try {
            val rows = FairyringRow.all()
            val codes = rows.map { it.code.replace(" ", "").uppercase() }.toSet()
            val missing = mutableListOf<String>()
            for (a in 0..3) for (b in 0..3) for (c in 0..3) {
                val code = FairyRingCode.of(a, b, c)
                if (code !in codes) missing += code
            }
            assertEquals(emptyList<String>(), missing)
            assertTrue(rows.isNotEmpty())
        } finally {
            cache.close()
        }
    }
}

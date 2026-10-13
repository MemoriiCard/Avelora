package org.rsmod.content.bosses.phosani

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.bosses.dsl.Magic
import org.rsmod.api.bosses.dsl.Ranged
import org.rsmod.api.bosses.validation.SpecValidator

@ResourceLock("server-cache")
class PhosaniTest {
    @Test
    fun `specs pass the boss validator`() {
        assertEquals(emptyList<Any>(), SpecValidator.validate(phosaniSpec()))
        val magic = phosaniHuskSpec(HUSK_MAGIC, "spotanim.nightmare_husk_magic_travel", Magic)
        val ranged = phosaniHuskSpec(HUSK_RANGED, "spotanim.nightmare_husk_ranged_travel", Ranged)
        assertEquals(emptyList<Any>(), SpecValidator.validate(magic))
        assertEquals(emptyList<Any>(), SpecValidator.validate(ranged))
    }

    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (npc in PHOSANI_TYPES + HUSK_MAGIC + HUSK_RANGED) {
                assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
            }
            assertEquals(3200, ServerCacheManager.getNpc(PHOSANI_FIRST.asRSCM(RSCMType.NPC))!!.hitpoints)
            for (loc in listOf("loc.nightmare_challenge_portal", "loc.nightmare_challenge_portal_enabled")) {
                assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            }
            for (seq in listOf(
                "seq.nightmare_attack_melee",
                "seq.nightmare_attack_magic",
                "seq.nightmare_attack_ranged",
                "seq.nightmare_attack_rift",
                "seq.nightmare_attack_summon",
                "seq.husk_magic_attack",
                "seq.husk_ranged_attack",
            )) {
                seq.asRSCM(RSCMType.SEQ)
            }
            for (spot in listOf(
                "spotanim.nightmare_rift",
                "spotanim.nightmare_magic_travel",
                "spotanim.nightmare_magic_impact",
                "spotanim.nightmare_ranged_travel",
                "spotanim.nightmare_husk_magic_travel",
                "spotanim.nightmare_husk_ranged_travel",
            )) {
                spot.asRSCM(RSCMType.SPOTANIM)
            }
            "varbit.nightmare_challenge_enabled".asRSCM(RSCMType.VARBIT)
        } finally {
            cache.close()
        }
    }
}

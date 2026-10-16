package org.rsmod.content.bosses.smokedevil

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect

internal const val THERMY = "npc.smoke_devil_boss"
internal const val SMOKE_DEVIL = "npc.smoke_devil"
internal const val SUPERIOR_SMOKE_DEVIL = "npc.superior_smoke_devil"

internal fun thermySpec(): BossSpec =
    boss(THERMY) {
        stats(attackRate = 2)
        val smoke =
            ability("smoke") {
                anim("seq.lore_dust_devil_attack")
                projectile(
                    spotanim = "spotanim.smoke_devil_smoke_proj",
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = Accuracy(Roll(0..8)), type = Ranged, penetration = 100),
                )
            }
        phase("combat") { weightedSelectorRandom { +random(smoke, weight = 1) } }
    }

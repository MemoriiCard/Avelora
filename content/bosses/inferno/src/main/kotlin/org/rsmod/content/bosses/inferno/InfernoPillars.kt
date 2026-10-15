package org.rsmod.content.bosses.inferno

import org.rsmod.map.CoordGrid

internal object InfernoPillars {
    val SYMBOLS = listOf("loc.inferno_safespot1", "loc.inferno_safespot2", "loc.inferno_safespot3")
    const val TARGET = "npc.inferno_invisible_3x3"
    val TILES = listOf(CoordGrid(2262, 5338, 0), CoordGrid(2276, 5336, 0), CoordGrid(2269, 5347, 0))
    const val SIZE = 3
    const val HITPOINTS = 255
    const val NIBBLE_MAX = 3
    const val TICK = 4
    const val REACH = 2
    const val COLLAPSE_RADIUS = 3
    const val COLLAPSE_MAX = 49

    val RESURRECTABLE =
        setOf(
            InfernoMonster.Bat.npc,
            InfernoMonster.Blob.npc,
            InfernoMonster.Meleer.npc,
            InfernoMonster.Ranger.npc,
        )
    const val REVIVE_DELAY = 6
    const val REVIVE_REACH = 15
    const val REVIVE_SEQ = "seq.jalakxil_resurrect"
    const val ZUK_SPAWN_SEQ = "seq.zuk_spawn"
    const val ZUK_SPAWN_TICKS = 8

    fun collapseDamage(roll: Int, distance: Int): Int =
        if (distance > COLLAPSE_RADIUS) 0 else roll.coerceIn(0, COLLAPSE_MAX)

    fun canRevive(type: String, alreadyRevived: Boolean, magerAlive: Boolean): Boolean =
        magerAlive && !alreadyRevived && type in RESURRECTABLE
}

package org.rsmod.content.raids.tob.nylocas

import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.content.raids.tob.party.TobScaling
import org.rsmod.map.CoordGrid

enum class NylocasStyle(val npcKey: String) {
    Melee("melee"),
    Ranged("ranged"),
    Magic("magic"),
}

enum class NylocasGate(val spawn: CoordGrid) {
    West(CoordGrid(3290, 4248, 0)),
    East(CoordGrid(3301, 4248, 0)),
    South(CoordGrid(3295, 4244, 0)),
}

data class NylocasSpawn(val gate: NylocasGate, val style: NylocasStyle, val big: Boolean) {
    val weight: Int
        get() = if (big) 2 else 1
}

object NylocasRules {
    const val WAVES = 31
    const val WAVE_GAP = 6
    const val PILLAR_HP = 200
    const val PILLAR_RATE = 4
    const val PILLAR_SMALL_MAX = 8
    const val PILLAR_BIG_MAX = 16
    const val PILLAR_COLLAPSE_MIN = 30
    const val PILLAR_COLLAPSE_MAX = 50
    const val COLLAPSE_RADIUS = 3
    const val MOB_RATE = 3
    const val MOB_MAX_HIT = 17
    const val MOB_RANGE = 4
    const val BOSS_THRESHOLD = 3
    const val BOSS_RATE = 4
    const val BOSS_MAX_HIT = 24
    const val STYLE_TICKS = 10
    const val BOSS_SPAWN_TICKS = 5

    val PILLARS =
        listOf(
            CoordGrid(3289, 4242, 0),
            CoordGrid(3300, 4242, 0),
            CoordGrid(3289, 4253, 0),
            CoordGrid(3300, 4253, 0),
        )

    val PILLAR_APPROACH =
        listOf(
            CoordGrid(3292, 4244, 0),
            CoordGrid(3299, 4244, 0),
            CoordGrid(3291, 4251, 0),
            CoordGrid(3298, 4251, 0),
        )

    fun aliveCap(teamSize: Int): Int =
        when {
            teamSize <= 3 -> 12
            teamSize == 4 -> 15
            else -> 18
        }

    fun waveSize(wave: Int): Int = 2 + wave / 8

    fun hasBig(wave: Int): Boolean = wave >= 4 && wave % 5 == 4

    fun wave(wave: Int, roll: (Int) -> Int): List<NylocasSpawn> {
        val gates = NylocasGate.entries
        val styles = NylocasStyle.entries
        val spawns =
            (0 until waveSize(wave)).map {
                NylocasSpawn(gates[(wave + it) % gates.size], styles[roll(styles.size)], false)
            }
        return if (hasBig(wave)) {
            spawns.dropLast(1) +
                NylocasSpawn(gates[roll(gates.size)], styles[roll(styles.size)], true)
        } else {
            spawns
        }
    }

    fun nextStyle(current: NylocasStyle?, roll: (Int) -> Int): NylocasStyle {
        val options = NylocasStyle.entries.filter { it != current }
        return options[roll(options.size)]
    }

    fun pillarDamage(big: Boolean, roll: (Int) -> Int): Int =
        1 + roll(if (big) PILLAR_BIG_MAX else PILLAR_SMALL_MAX)

    fun collapseDamage(roll: (Int) -> Int, mode: TobMode): Int =
        TobScaling.damage(PILLAR_COLLAPSE_MIN + roll(PILLAR_COLLAPSE_MAX - PILLAR_COLLAPSE_MIN + 1), mode)

    fun readyForBoss(wavesSpawned: Int, mobsLeft: Int): Boolean =
        wavesSpawned >= WAVES && mobsLeft <= BOSS_THRESHOLD

    /** True when [damageType] is the style the boss's current colour is weak to. */
    fun matches(form: NylocasStyle, damageType: NylocasStyle): Boolean = form == damageType
}

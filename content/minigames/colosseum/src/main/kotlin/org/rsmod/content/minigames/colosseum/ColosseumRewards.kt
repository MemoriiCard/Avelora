package org.rsmod.content.minigames.colosseum

internal object ColosseumRewards {
    class Reward(val obj: String, val count: Int)

    const val FIRST_UNIQUE_WAVE = 4
    const val TONALZTICS_WAVE = 7
    const val SPLINTERS = "obj.sunfiresplinter"
    const val SPLINTER_BASE = 50
    const val SPLINTER_PER_WAVE = 35
    const val SPLINTER_SPREAD = 50
    const val UNIQUE_BASE_ODDS = 90
    const val UNIQUE_ODDS_STEP = 7
    const val UNIQUE_MIN_ODDS = 30
    const val PET_ODDS = 200
    const val QUIVER = "obj.dizanas_quiver_uncharged"
    const val PET = "obj.solhereditpet"

    private val armour = listOf("obj.sunfire_helm", "obj.sunfire_body", "obj.sunfire_legs")

    fun forWave(wave: Int, roll: (Int) -> Int): List<Reward> {
        val rewards = ArrayList<Reward>()
        rewards += Reward(SPLINTERS, SPLINTER_BASE + SPLINTER_PER_WAVE * wave + roll(SPLINTER_SPREAD))
        if (wave < FIRST_UNIQUE_WAVE) return rewards
        val odds = (UNIQUE_BASE_ODDS - UNIQUE_ODDS_STEP * (wave - FIRST_UNIQUE_WAVE)).coerceAtLeast(UNIQUE_MIN_ODDS)
        if (roll(odds) != 0) return rewards
        val pick = roll(16)
        rewards +=
            when {
                pick < 9 -> Reward(armour[pick % armour.size], 1)
                pick < 15 -> Reward("obj.echo_crystal", 1)
                wave >= TONALZTICS_WAVE -> Reward("obj.tonalztics_of_ralos_uncharged", 1)
                else -> Reward("obj.echo_crystal", 1)
            }
        return rewards
    }

    fun completion(roll: (Int) -> Int): List<Reward> {
        val rewards = arrayListOf(Reward(QUIVER, 1))
        if (roll(PET_ODDS) == 0) rewards += Reward(PET, 1)
        return rewards
    }

    fun glory(wave: Int, modifiers: Set<ColosseumModifier>): Int {
        val base = GLORY_BASE + GLORY_PER_WAVE * (wave - 1)
        val bonus = modifiers.sumOf { it.glory }
        return base * (100 + bonus) / 100
    }

    private const val GLORY_BASE = 100
    private const val GLORY_PER_WAVE = 30
}

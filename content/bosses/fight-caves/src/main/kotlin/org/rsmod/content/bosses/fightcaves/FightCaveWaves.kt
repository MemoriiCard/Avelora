package org.rsmod.content.bosses.fightcaves

internal enum class FightCaveMonster(val npc: String, val ranged: Boolean) {
    TzKih("npc.tzhaar_fightcave_swarm_1a", ranged = false),
    TzKek("npc.tzhaar_fightcave_swarm_2a", ranged = false),
    TokXil("npc.tzhaar_fightcave_swarm_3a", ranged = true),
    YtMejKot("npc.tzhaar_fightcave_swarm_4a", ranged = false),
    KetZek("npc.tzhaar_fightcave_swarm_5a", ranged = true),
    TzTokJad("npc.tzhaar_fightcave_swarm_boss", ranged = true),
}

internal object FightCaveWaves {
    const val FINAL_WAVE = 63
    const val JAD_BONUS_TOKKUL = 4000

    private val waves: List<List<FightCaveMonster>> = buildWaves()

    fun wave(number: Int): List<FightCaveMonster> = waves[number - 1]

    fun tokkulFor(wavesCompleted: Int): Int {
        val base = wavesCompleted * (wavesCompleted + 1)
        return if (wavesCompleted >= FINAL_WAVE) base + JAD_BONUS_TOKKUL else base
    }

    /**
     * Each tier repeats every earlier wave with one of itself added, bracketed by a lone spawn and
     * a pair: 1 kih, 2 kih, then kek, kek + each earlier wave, 2 kek, and so on up to Ket-Zek,
     * giving 62 waves before Jad.
     */
    private fun buildWaves(): List<List<FightCaveMonster>> {
        var waves = emptyList<List<FightCaveMonster>>()
        for (monster in FightCaveMonster.entries - FightCaveMonster.TzTokJad) {
            waves = waves + listOf(listOf(monster)) + waves.map { listOf(monster) + it } + listOf(listOf(monster, monster))
        }
        return waves + listOf(listOf(FightCaveMonster.TzTokJad))
    }
}

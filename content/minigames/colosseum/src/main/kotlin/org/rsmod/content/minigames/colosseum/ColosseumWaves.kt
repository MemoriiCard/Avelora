package org.rsmod.content.minigames.colosseum

internal enum class ColosseumMonster(
    val npc: String,
    val deathSeq: String,
    val deathTicks: Int,
) {
    Berserker("npc.colosseum_warbander_melee_male", "seq.npc_fremennik_warband_melee_human_death", 3),
    Seer("npc.colosseum_warbander_mage_male", "seq.npc_fremennik_warband_mage_human_death", 3),
    Archer("npc.colosseum_warbander_ranged_female", "seq.npc_fremennik_warband_archer_human_death", 3),
    Shaman("npc.colosseum_standard_mager", "seq.npc_serpent_mager_death", 3),
    Jaguar("npc.colosseum_jaguar_warrior", "seq.npc_jaguar_combat_death", 4),
    Javelin("npc.colosseum_javelin_colossus", "seq.npc_colossi_javelin_01_death", 4),
    Manticore("npc.colosseum_manticore", "seq.npc_manticore_01_death", 4),
    Shockwave("npc.colosseum_shockwave_colossus", "seq.npc_colossi_shockwave_01_death", 4),
    Minotaur("npc.colosseum_minotaur", "seq.npc_minotaur_boss_death", 4),
    MinotaurRoute("npc.colosseum_minotaur_routefind", "seq.npc_minotaur_boss_death", 4),
    Sol("npc.colosseum_sol_p1", "seq.npc_colossi_finalboss_01_death", 6),
    ;

    companion object {
        val warband = listOf(Berserker, Seer, Archer)
    }
}

internal object ColosseumWaves {
    const val SOL_WAVE = 12
    const val FINAL_REGULAR_WAVE = 11
    const val REINFORCEMENT_TICKS = 67

    private class Spec(
        val shaman: Int = 0,
        val javelin: Int = 0,
        val manticore: Int = 0,
        val shockwave: Int = 0,
        val jaguar: Int = 0,
        val minotaur: Int = 0,
        val reinforcementShaman: Int = 0,
    )

    private val TABLE =
        listOf(
            Spec(shaman = 1, jaguar = 1),
            Spec(shaman = 1, javelin = 1, jaguar = 1),
            Spec(shaman = 1, javelin = 2, jaguar = 1),
            Spec(shaman = 1, manticore = 1, jaguar = 1, reinforcementShaman = 1),
            Spec(shaman = 1, javelin = 1, manticore = 1, jaguar = 1, reinforcementShaman = 1),
            Spec(shaman = 1, javelin = 2, manticore = 1, jaguar = 1, reinforcementShaman = 1),
            Spec(javelin = 1, manticore = 1, shockwave = 1, minotaur = 1),
            Spec(javelin = 2, manticore = 1, shockwave = 1, minotaur = 1),
            Spec(javelin = 1, manticore = 2, minotaur = 1),
            Spec(javelin = 2, manticore = 2, minotaur = 1, reinforcementShaman = 1),
            Spec(javelin = 1, manticore = 2, shockwave = 1, minotaur = 1, reinforcementShaman = 1),
        )

    fun main(wave: Int, quartet: Boolean = false, dynamicDuo: Boolean = false): List<ColosseumMonster> {
        if (wave == SOL_WAVE) return listOf(ColosseumMonster.Sol)
        val spec = TABLE[wave - 1]
        val list = ArrayList<ColosseumMonster>(ColosseumMonster.warband)
        if (quartet) list += ColosseumMonster.warband[wave % ColosseumMonster.warband.size]
        repeat(spec.shaman) { list += ColosseumMonster.Shaman }
        repeat(spec.javelin) { list += ColosseumMonster.Javelin }
        repeat(spec.manticore) { list += ColosseumMonster.Manticore }
        repeat(spec.shockwave) { list += ColosseumMonster.Shockwave }
        if (dynamicDuo && spec.shockwave > 0) repeat(spec.shockwave) { list += ColosseumMonster.Shockwave }
        return list
    }

    fun reinforcements(wave: Int): List<ColosseumMonster> {
        if (wave == SOL_WAVE) return emptyList()
        val spec = TABLE[wave - 1]
        val list = ArrayList<ColosseumMonster>()
        repeat(spec.jaguar) { list += ColosseumMonster.Jaguar }
        repeat(spec.minotaur) { list += ColosseumMonster.Minotaur }
        repeat(spec.reinforcementShaman) { list += ColosseumMonster.Shaman }
        return list
    }
}

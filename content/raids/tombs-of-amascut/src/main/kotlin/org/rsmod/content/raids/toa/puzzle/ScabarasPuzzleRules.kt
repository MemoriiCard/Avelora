package org.rsmod.content.raids.toa.puzzle

object ScabarasPuzzleRules {
    const val WAVES = 3
    const val SCARAB_HP = 12
    const val HIT = 8
    const val RATE = 4

    fun waveSize(wave: Int, teamSize: Int): Int = 3 + wave * 2 + teamSize
}

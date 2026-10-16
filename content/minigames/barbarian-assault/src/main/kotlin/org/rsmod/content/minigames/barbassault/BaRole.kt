package org.rsmod.content.minigames.barbassault

enum class BaKind(val targetedBy: BaRole?) {
    Fighter(BaRole.Attacker),
    Ranger(BaRole.Attacker),
    Runner(BaRole.Defender),
    Healer(BaRole.Healer),
    Spawn(BaRole.Attacker),
    Queen(null),
}

enum class BaRole(val label: String, val varp: String) {
    Attacker("Attacker", "varp.ba_points_attacker"),
    Defender("Defender", "varp.ba_points_defender"),
    Healer("Healer", "varp.ba_points_healer"),
    Collector("Collector", "varp.ba_points_collector"),
}

object BaLevels {
    val THRESHOLDS = intArrayOf(0, 200, 500, 900, 1400)
    val HEAL_AMOUNTS = intArrayOf(10, 15, 20, 25, 35)
    const val MAX_LEVEL = 5

    fun level(points: Int): Int = THRESHOLDS.indexOfLast { points >= it } + 1

    fun healAmount(level: Int): Int = HEAL_AMOUNTS[level.coerceIn(1, MAX_LEVEL) - 1]

    fun lureRange(level: Int): Int = if (level >= 2) 5 else 4
}

class BaWaveStats {
    var kills = 0
    var escapes = 0
    var healed = 0
    var eggs = 0

    fun reset() {
        kills = 0
        escapes = 0
        healed = 0
        eggs = 0
    }
}

object BaScoring {
    const val FINAL_WAVE_ROLE = 80
    const val FINAL_WAVE_OTHER = 5
    private const val ESCAPE_PENALTY_CAP = 10
    private const val HEAL_CAP = 28
    private const val EGG_CAP = 60
    private const val EGG_DIVISOR = 4.35

    fun wavePoints(role: BaRole, wave: Int, stats: BaWaveStats): Int {
        val x = wave + 1
        val raw =
            when (role) {
                BaRole.Attacker -> stats.kills * (x - 1.0)
                BaRole.Defender ->
                    stats.kills * 1.8 * x - minOf(ESCAPE_PENALTY_CAP.toDouble(), stats.escapes * 3.0 * x)
                BaRole.Healer ->
                    stats.kills * 0.7 * x + minOf(HEAL_CAP.toDouble(), stats.healed / 18.0)
                BaRole.Collector -> minOf(stats.eggs, EGG_CAP) / EGG_DIVISOR
            }
        return raw.toInt().coerceAtLeast(0)
    }
}

object BaWave {
    const val FINAL = 10

    fun compose(wave: Int, players: Int): List<BaKind> {
        val extra = players - 1
        if (wave >= FINAL) return listOf(BaKind.Queen) + List(players) { BaKind.Spawn }
        val healers = if (wave < 2) 0 else 1 + wave / 4 + extra / 2
        return List(2 + wave / 2 + extra) { BaKind.Fighter } +
            List(1 + wave / 3 + extra / 2) { BaKind.Ranger } +
            List(2 + wave / 2 + extra) { BaKind.Runner } +
            List(healers) { BaKind.Healer }
    }
}

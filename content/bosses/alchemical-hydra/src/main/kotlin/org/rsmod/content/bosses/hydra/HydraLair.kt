package org.rsmod.content.bosses.hydra

import org.rsmod.map.CoordGrid

internal object HydraLair {
    const val REGION = 5536
    const val SLAYER_LEVEL = 95

    val OUTSIDE = CoordGrid(1354, 10259, 0)
    val INSIDE = CoordGrid(1356, 10259, 0)
    val SPAWN = CoordGrid(1364, 10265, 0)
    val CLIMB_SOUTH = CoordGrid(1351, 10250, 0)
    val CLIMB_NORTH = CoordGrid(1351, 10252, 0)

    val CORNERS =
        listOf(
            CoordGrid(1357, 10257, 0),
            CoordGrid(1375, 10257, 0),
            CoordGrid(1358, 10278, 0),
            CoordGrid(1375, 10278, 0),
        )
    val CENTRE = CoordGrid(1364, 10265, 0)

    const val MIN_X = 1356
    const val MAX_X = 1377
    const val MIN_Z = 10257
    const val MAX_Z = 10278
}

internal enum class HydraVent(val tile: CoordGrid) {
    Red(CoordGrid(1371, 10263, 0)),
    Green(CoordGrid(1371, 10272, 0)),
    Blue(CoordGrid(1362, 10272, 0)),
}

internal enum class HydraPhase(
    val npc: String,
    val transition: String?,
    val weakness: HydraVent?,
    val stage: Int,
) {
    Serpentine("npc.hydraboss", "npc.hydraboss_p1_transition", HydraVent.Red, 1),
    Electric("npc.hydraboss_4", "npc.hydraboss_p2_transition", HydraVent.Green, 2),
    Flame("npc.hydraboss_3", "npc.hydraboss_p3_transition", HydraVent.Blue, 3),
    Enraged("npc.hydraboss_2", null, null, 4);

    val next: HydraPhase?
        get() = entries.getOrNull(ordinal + 1)

    val spawnSeq: String
        get() = "seq.hydra_stage_${stage}_spawn"

    val transitionSeq: String
        get() = "seq.hydra_stage_${stage}_death"

    val specialSeq: String
        get() = if (this == Enraged) "seq.hydra_stage_4_attack_magic" else "seq.hydra_stage_${stage}_attack_special"
}

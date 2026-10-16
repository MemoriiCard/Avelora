package org.rsmod.content.minigames.barbassault

internal object BaMonsters {
    private const val QUEEN = "npc.barbassault_pen_queen_new"
    private const val SPAWN = "npc.barbassault_pen_queen_spawn"
    private val KINDS =
        mapOf(
            BaKind.Fighter to "fighter",
            BaKind.Ranger to "ranger",
            BaKind.Runner to "runner",
            BaKind.Healer to "healer",
        )

    fun level(wave: Int): Int = wave.coerceIn(1, 9)

    fun symbol(kind: BaKind, wave: Int): String =
        when (kind) {
            BaKind.Queen -> QUEEN
            BaKind.Spawn -> SPAWN
            else -> "npc.barbassault_pen_${KINDS.getValue(kind)}_lv${level(wave)}"
        }

    val ALL: List<String> =
        KINDS.values.flatMap { kind -> (1..9).map { "npc.barbassault_pen_${kind}_lv$it" } } +
            listOf(QUEEN, SPAWN)
}

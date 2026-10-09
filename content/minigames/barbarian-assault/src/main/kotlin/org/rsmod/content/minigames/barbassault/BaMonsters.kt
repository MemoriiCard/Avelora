package org.rsmod.content.minigames.barbassault

internal object BaMonsters {
    private val KINDS = listOf("fighter", "ranger", "runner")

    fun level(wave: Int): Int = (wave * 2 - 1).coerceIn(1, 9)

    fun symbol(wave: Int, index: Int): String =
        "npc.barbassault_pen_${KINDS[index % KINDS.size]}_lv${level(wave)}"

    val ALL: List<String> =
        KINDS.flatMap { kind -> (1..9).map { "npc.barbassault_pen_${kind}_lv$it" } }
}

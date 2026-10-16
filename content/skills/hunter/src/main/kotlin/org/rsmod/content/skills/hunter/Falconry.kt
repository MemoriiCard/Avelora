package org.rsmod.content.skills.hunter

class Kebbit(
    val npc: String,
    val name: String,
    val level: Int,
    val xp: Double,
    val fur: String,
    val catchLow: Int,
    val catchHigh: Int,
)

object Falconry {
    const val GLOVE = "obj.falcon_gloves"
    const val GLOVE_PRICE = 500
    const val GLOVE_LEVEL = 43
    const val COINS = "obj.coins"
    const val BONES = "obj.bones"

    val kebbits =
        listOf(
            Kebbit("npc.huntingbeast_speedy", "spotted kebbit", 43, 104.0, "obj.huntingbeast_speedy_fur", 100, 250),
            Kebbit("npc.huntingbeast_silent", "dark kebbit", 57, 132.0, "obj.huntingbeast_silent_fur", 80, 240),
            Kebbit("npc.huntingbeast_speedy2", "dashing kebbit", 69, 156.0, "obj.huntingbeast_speedy2_fur", 60, 220),
        )
}

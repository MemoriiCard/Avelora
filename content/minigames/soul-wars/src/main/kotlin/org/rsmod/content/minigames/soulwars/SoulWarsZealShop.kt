package org.rsmod.content.minigames.soulwars

internal enum class ZealSkill(val label: String, val stat: String, val multiplier: Int) {
    Attack("Attack", "stat.attack", 30),
    Strength("Strength", "stat.strength", 30),
    Defence("Defence", "stat.defence", 30),
    Hitpoints("Hitpoints", "stat.hitpoints", 30),
    Ranged("Ranged", "stat.ranged", 27),
    Magic("Magic", "stat.magic", 27),
    Prayer("Prayer", "stat.prayer", 14),
}

internal class ZealStock(val label: String, val obj: String, val quantity: Int, val cost: Int)

internal object ZealShop {
    const val MIN_LEVEL = 30

    fun xpPerZeal(skill: ZealSkill, level: Int): Int =
        if (level < MIN_LEVEL) 0 else level * level / 600 * skill.multiplier

    val SUPPLIES =
        listOf(
            ZealStock("Blighted manta ray (x15)", "obj.blighted_mantaray", 15, 10),
            ZealStock("Blighted anglerfish (x15)", "obj.blighted_anglerfish", 15, 10),
            ZealStock("Blighted karambwan (x12)", "obj.blighted_karambwan", 12, 10),
            ZealStock("Blighted super restore(4) (x4)", "obj.blighted_4dose2restore", 4, 10),
        )

    val SACKS =
        listOf(
            ZealStock("Blighted entangle sack (x70)", "obj.blighted_sack_entangle", 70, 10),
            ZealStock("Blighted teleport spell sack (x50)", "obj.blighted_sack_teleblock", 50, 10),
            ZealStock("Blighted vengeance sack (x50)", "obj.blighted_sack_vengeance", 50, 10),
            ZealStock("Blighted ancient ice sack (x30)", "obj.blighted_sack_icebarrage", 30, 10),
        )

    val SPECIAL =
        listOf(
            ZealStock("Soul cape", "obj.soul_cape_blue", 1, 2500),
            ZealStock("Ectoplasmator", "obj.soul_wars_ectoplasmator", 1, 250),
            ZealStock("Spoils of War", "obj.soul_wars_spoils", 1, 30),
        )

    val ALL = SUPPLIES + SACKS + SPECIAL
}

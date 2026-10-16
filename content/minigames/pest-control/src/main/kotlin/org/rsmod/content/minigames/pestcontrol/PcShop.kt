package org.rsmod.content.minigames.pestcontrol

internal enum class PcSkill(val label: String, val stat: String, val multiplier: Int) {
    Attack("Attack", "stat.attack", 35),
    Strength("Strength", "stat.strength", 35),
    Defence("Defence", "stat.defence", 35),
    Hitpoints("Hitpoints", "stat.hitpoints", 35),
    Ranged("Ranged", "stat.ranged", 32),
    Magic("Magic", "stat.magic", 32),
    Prayer("Prayer", "stat.prayer", 18),
}

internal class PcStock(val label: String, val obj: String, val cost: Int)

internal class PcUpgrade(val label: String, val from: String, val to: String, val cost: Int)

internal object PcShop {
    const val MIN_EXCHANGE_LEVEL = 25
    const val MIN_COMBAT_STAT = 42
    const val MIN_PRAYER = 22
    const val POINT_CAP = 4000

    fun xpPerPoint(skill: PcSkill, level: Int): Int =
        if (level < MIN_EXCHANGE_LEVEL) 0 else level * level / 600 * skill.multiplier

    fun bulkBonusPercent(points: Int): Int =
        when {
            points >= 100 -> 10
            points >= 10 -> 1
            else -> 0
        }

    fun experience(skill: PcSkill, level: Int, points: Int): Long {
        val base = xpPerPoint(skill, level).toLong() * points
        return base + base * bulkBonusPercent(points) / 100
    }

    val VOID =
        listOf(
            PcStock("Void knight top", "obj.pest_void_knight_top", 250),
            PcStock("Void knight robe", "obj.pest_void_knight_robes", 250),
            PcStock("Void knight gloves", "obj.pest_void_knight_gloves", 150),
            PcStock("Void ranger helm", "obj.game_pest_archer_helm", 200),
            PcStock("Void mage helm", "obj.game_pest_mage_helm", 200),
            PcStock("Void melee helm", "obj.game_pest_melee_helm", 200),
            PcStock("Void knight mace", "obj.pest_void_knight_mace", 250),
        )

    val SEAL = PcStock("Void seal", PcSeal.symbol(PcSeal.FULL_CHARGES), PcSeal.COST)

    val ELITE =
        listOf(
            PcUpgrade("Elite void top", "obj.pest_void_knight_top", "obj.elite_void_knight_top", 200),
            PcUpgrade("Elite void robe", "obj.pest_void_knight_robes", "obj.elite_void_knight_robes", 200),
        )

    val SKILLS = PcSkill.entries
}

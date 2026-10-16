package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Mixes the raid's gourd potions. A water-filled gourd vial takes one herb and one secondary; the
 * potion comes out weak, standard or strong depending on the maker's Herblore level. Overloads are
 * made by adding a noxifer to an elder, twisted and kodai potion of the same strength.
 */
class PotionBrewingScript @Inject constructor(private val supplyPoints: CoxSupplyPoints) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (herb in HERBS) onOpHeldU(VIAL_WATER, herb) { mix(herb = it.second.internalName) }
        for (secondary in SECONDARIES) {
            onOpHeldU(VIAL_WATER, secondary) { mix(secondary = it.second.internalName) }
        }
        for (strength in Strength.entries) {
            for (base in OVERLOAD_BASES) {
                onOpHeldU(NOXIFER, strength.item(base, FULL)) { overload() }
            }
        }
    }

    private fun ProtectedAccess.mix(herb: String? = null, secondary: String? = null) {
        val recipe =
            RECIPES.firstOrNull {
                (herb == null || it.herb == herb) &&
                    (secondary == null || it.secondary == secondary) &&
                    inv.count(it.herb) > 0 &&
                    inv.count(it.secondary!!) > 0
            }
        if (recipe == null) {
            mes("You need a herb and a secondary ingredient to mix a potion.")
            return
        }
        val strength = recipe.strengthFor(player.herbloreLvl)
        if (strength == null) {
            mes("You need a Herblore level of ${recipe.levels[0]} to make this potion.")
            return
        }
        for (ingredient in listOf(VIAL_WATER, recipe.herb, recipe.secondary!!)) {
            if (invDel(inv, ingredient).failure) return
        }
        invAdd(inv, strength.item(recipe.key, FULL), strict = false)
        statAdvance("stat.herblore", recipe.xp[strength.ordinal])
        supplyPoints.potion(player, FULL, recipe.pointsPerDose + strength.ordinal * DOSE_POINT_STEP)
        mes("You mix the ${recipe.key} potion.")
    }

    private fun ProtectedAccess.overload() {
        val recipe = OVERLOAD
        val maker = recipe.strengthFor(player.herbloreLvl)
        if (maker == null) {
            mes("You need a Herblore level of ${recipe.levels[0]} to make an overload.")
            return
        }
        val strength =
            Strength.entries.lastOrNull { s ->
                s <= maker && OVERLOAD_BASES.all { inv.count(s.item(it, FULL)) > 0 }
            }
        if (strength == null) {
            mes("You need an elder, twisted and kodai potion of the same strength and a noxifer.")
            return
        }
        for (base in OVERLOAD_BASES) {
            if (invDel(inv, strength.item(base, FULL)).failure) return
        }
        if (invDel(inv, NOXIFER).failure) return
        invAdd(inv, strength.item(recipe.key, FULL), strict = false)
        statAdvance("stat.herblore", recipe.xp[strength.ordinal])
        supplyPoints.potion(player, FULL, recipe.pointsPerDose + strength.ordinal * DOSE_POINT_STEP)
        mes("You mix the overload potion.")
    }

    enum class Strength(private val infix: String) {
        Weak("_weak"),
        Standard(""),
        Strong("_strong"),
        ;

        fun item(key: String, doses: Int): String = "obj.raids_vial_$key${infix}_$doses"
    }

    class Recipe(
        val key: String,
        val herb: String,
        val secondary: String?,
        val levels: List<Int>,
        val xp: List<Double>,
        val pointsPerDose: Int,
    ) {
        fun strengthFor(level: Int): Strength? =
            Strength.entries.lastOrNull { level >= levels[it.ordinal] }
    }

    companion object {
        const val VIAL_WATER = "obj.raids_vial_water"
        const val NOXIFER = "obj.raids_noxifer"
        const val FULL = 4

        private const val GOLPAR = "obj.raids_golpar"
        private const val BUCHU = "obj.raids_buchuleaf"
        private const val STINKHORN = "obj.raids_stinkhorn_mushroom"
        private const val CICELY = "obj.raids_cicely"
        private const val JUICE = "obj.raids_endarkened_juice"
        private const val DOSE_POINT_STEP = 4

        private val HERBS = listOf(GOLPAR, BUCHU, NOXIFER)
        private val SECONDARIES = listOf(STINKHORN, CICELY, JUICE)
        private val OVERLOAD_BASES = listOf("elder", "twisted", "kodai")

        private val BASIC_LEVELS = listOf(47, 59, 70)
        private val BASIC_XP = listOf(6.5, 10.0, 13.0)
        private val ADVANCED_LEVELS = listOf(52, 65, 78)
        private val ADVANCED_XP = listOf(13.5, 20.0, 26.5)
        private val TOP_LEVELS = listOf(60, 75, 90)

        val RECIPES =
            listOf(
                Recipe("elder", GOLPAR, STINKHORN, BASIC_LEVELS, BASIC_XP, 8),
                Recipe("twisted", GOLPAR, CICELY, BASIC_LEVELS, BASIC_XP, 8),
                Recipe("kodai", GOLPAR, JUICE, BASIC_LEVELS, BASIC_XP, 8),
                Recipe("revitalisation", BUCHU, STINKHORN, ADVANCED_LEVELS, ADVANCED_XP, 8),
                Recipe("prayer", BUCHU, CICELY, ADVANCED_LEVELS, ADVANCED_XP, 8),
                Recipe("xericaid", BUCHU, JUICE, ADVANCED_LEVELS, ADVANCED_XP, 8),
                Recipe("antipoison", NOXIFER, CICELY, TOP_LEVELS, ADVANCED_XP, 4),
            )

        val OVERLOAD =
            Recipe("overload", NOXIFER, null, TOP_LEVELS, listOf(33.5, 50.0, 66.5), 16)
    }
}

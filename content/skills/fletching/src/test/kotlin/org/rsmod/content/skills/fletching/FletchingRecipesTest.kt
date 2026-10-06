package org.rsmod.content.skills.fletching

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.rsmod.content.skills.fletching.FletchingRecipes.maxActions
import org.rsmod.content.skills.fletching.FletchingRecipes.setSize

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FletchingRecipesTest {
    @BeforeAll
    fun loadCache() {
        ServerCacheManager.init(240).close()
    }

    @Test
    fun `every recipe references real objs and animations`() {
        for (recipe in FletchingRecipes.all) {
            val objs = recipe.inputs.map { it.obj } + recipe.output + listOfNotNull(recipe.tool)
            for (obj in objs) {
                assertNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)), obj)
            }
            assertNotNull(ServerCacheManager.getAnim(recipe.anim.asRSCM(RSCMType.SEQ)), recipe.anim)
        }
    }

    @Test
    fun `log cutting matches the wiki`() {
        val magic = FletchingRecipes.logCutting.getValue("obj.magic_logs")
        val longbow = magic.single { it.output == "obj.unstrung_magic_longbow" }
        assertEquals(85, longbow.level)
        assertEquals(91.5, longbow.xp)
        val shafts = FletchingRecipes.logCutting.getValue("obj.redwood_logs").first()
        assertEquals(105, shafts.outputCount)
        assertEquals(35.0, shafts.xp)
        val shield = FletchingRecipes.logCutting.getValue("obj.yew_logs").single { it.output == "obj.yew_shield" }
        assertEquals(2, shield.inputs.single().count)
        assertEquals(7, shield.ticks)
    }

    @Test
    fun `arrow sets make up to fifteen and round partial sets up`() {
        val bronze = FletchingRecipes.arrows.first()
        val counts = mapOf("obj.headless_arrow" to 40, "obj.bronze_arrowheads" to 100)
        assertEquals(3, bronze.maxActions { counts[it] ?: 0 })
        assertEquals(15, bronze.setSize { counts[it] ?: 0 })
        val leftover = mapOf("obj.headless_arrow" to 10, "obj.bronze_arrowheads" to 100)
        assertEquals(10, bronze.setSize { leftover[it] ?: 0 })
    }

    @Test
    fun `shields need two logs per action`() {
        val oak = FletchingRecipes.logCutting.getValue("obj.oak_logs").single { it.output == "obj.oak_shield" }
        assertEquals(2, oak.maxActions { if (it == "obj.oak_logs") 5 else 0 })
    }

    @Test
    fun `no two recipe groups share a pair of items in either order`() {
        val triggers = FletchingRecipes.all.map { it.trigger }.distinct()
        val unordered = triggers.map { setOf(it.first, it.second) }
        assertEquals(unordered.size, unordered.distinct().size)
    }

    @Test
    fun `bolts darts and javelins are one-click sets`() {
        val ammo = AmmoRecipes.bolts + AmmoRecipes.darts + AmmoRecipes.javelins
        for (recipe in ammo) {
            assertEquals(true, recipe.isInstant, recipe.output)
            assertEquals(true, recipe.isSet, recipe.output)
        }
        val dragonDart = AmmoRecipes.darts.single { it.output == "obj.dragon_dart" }
        assertEquals(95, dragonDart.level)
        assertEquals(25.0, dragonDart.xp)
    }

    @Test
    fun `crossbows need a hammer for limbs and match the wiki`() {
        val rune = CrossbowRecipes.limbs.single { it.output == "obj.xbows_crossbow_unstrung_runite" }
        assertEquals("obj.hammer", rune.tool)
        assertEquals(69, rune.level)
        assertEquals(100.0, rune.xp)
        val dragon = CrossbowRecipes.stringing.single { it.output == "obj.xbows_crossbow_dragon" }
        assertEquals(78, dragon.level)
        assertEquals(70.0, dragon.xp)
    }
}

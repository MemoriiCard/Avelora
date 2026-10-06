package org.rsmod.content.skills.fletching

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.fletching.FletchingRecipes.maxActions
import org.rsmod.content.skills.fletching.FletchingRecipes.setSize
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FletchingScript : PluginScript() {
    override fun ScriptContext.startup() {
        for ((log, recipes) in FletchingRecipes.logCutting) {
            onOpHeldU(FletchingRecipes.KNIFE, log) {
                chooseAndFletch(recipes, SkillingActionType.MAKE, "make")
            }
        }
        for (recipe in FletchingRecipes.bowStringing) {
            onOpHeldU(FletchingRecipes.BOW_STRING, recipe.inputs.first().obj) {
                chooseAndFletch(listOf(recipe), SkillingActionType.STRING, "string")
            }
        }
        onOpHeldU(FletchingRecipes.ARROW_SHAFT, FletchingRecipes.FEATHER) {
            chooseAndFletch(listOf(FletchingRecipes.headlessArrows), SkillingActionType.MAKE_SETS, "make")
        }
        for (recipe in FletchingRecipes.arrows) {
            onOpHeldU(FletchingRecipes.HEADLESS_ARROW, recipe.inputs.last().obj) {
                chooseAndFletch(listOf(recipe), SkillingActionType.MAKE_SETS, "make")
            }
        }
        onPlayerQueueWithArgs<FletchTask>(QUEUE) { fletchOnce(it.args) }
    }

    private suspend fun ProtectedAccess.chooseAndFletch(
        recipes: List<FletchRecipe>,
        actionType: SkillingActionType,
        verb: String,
    ) {
        val byOutput = recipes.associateBy { it.output }
        val config =
            SkillMultiConfig(
                actionType = actionType,
                verb = verb,
                entries = recipes.map { SkillMultiEntry(it.output) },
                maxCountProvider = { inventory, entry ->
                    byOutput.getValue(entry.internal).maxActions(inventory::count)
                },
            )
        openSkillMulti(config) { selection ->
            val recipe = byOutput.getValue(selection.entry.internal)
            if (!hasLevel(recipe)) {
                mesbox("You need a Fletching level of ${recipe.level} to make that.")
                return@openSkillMulti
            }
            anim(recipe.anim)
            weakQueue(QUEUE, recipe.firstTicks + QUEUE_COMPENSATION, FletchTask(recipe, selection.amount))
        }
    }

    private fun ProtectedAccess.fletchOnce(task: FletchTask) {
        val recipe = task.recipe
        if (!hasLevel(recipe) || !hasTool(recipe) || recipe.maxActions(inv::count) <= 0) {
            resetAnim()
            return
        }

        val made = if (recipe.isSet) recipe.setSize(inv::count) else recipe.outputCount
        val perInput = if (recipe.isSet) made else 1
        val result =
            player.invTransaction(inv) {
                val from = select(inv)
                for (input in recipe.inputs) {
                    delete {
                        this.from = from
                        this.obj = input.obj.asRSCM(RSCMType.OBJ)
                        this.strictCount = input.count * perInput
                    }
                }
                insert {
                    this.into = from
                    this.obj = recipe.output.asRSCM(RSCMType.OBJ)
                    this.strictCount = made
                }
            }
        if (result.failure) {
            mes("You don't have enough inventory space to do that.")
            resetAnim()
            return
        }

        val xp = if (recipe.isSet) recipe.xp * made else recipe.xp
        statAdvance(STAT, xp)
        recipe.sound?.let { soundSynth(it) }
        mes(recipe.message.replace("{count}", made.toString()), ChatType.Spam)

        val done = task.done + 1
        if (done >= task.amount || recipe.maxActions(inv::count) <= 0) {
            return
        }
        anim(recipe.anim)
        weakQueue(QUEUE, recipe.ticks + QUEUE_COMPENSATION, task.copy(done = done))
    }

    private fun ProtectedAccess.hasLevel(recipe: FletchRecipe): Boolean =
        statBase(STAT) >= recipe.level

    private fun ProtectedAccess.hasTool(recipe: FletchRecipe): Boolean =
        recipe.tool == null || inv.contains(recipe.tool)

    private data class FletchTask(val recipe: FletchRecipe, val amount: Int, val done: Int = 0)

    private companion object {
        const val QUEUE = "queue.fletching_make"
        const val STAT = "stat.fletching"

        /** A queue scheduled from inside a queue handler ticks down once in that same cycle. */
        const val QUEUE_COMPENSATION = 1
    }
}

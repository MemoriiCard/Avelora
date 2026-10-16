package org.rsmod.content.minigames.foundry

import jakarta.inject.Inject
import kotlin.math.roundToInt
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.smithingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class FoundryScript
@Inject
internal constructor(
    private val random: GameRandom,
    private val objs: ObjRepository,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.giants_foundry_entrance") { enter() }
        onOpLoc1("loc.giants_foundry_exit") { leave() }
        onOpLoc1("loc.giants_foundry_crucible_multi") {
            if (player.crucibleState == CRUCIBLE_FULL) pour() else fill()
        }
        onOpLoc2("loc.giants_foundry_crucible_multi") { checkCrucible() }
        onOpLoc3("loc.giants_foundry_crucible_multi") { emptyCrucible() }
        onOpLoc1("loc.giants_foundry_mould_jig") { jig() }
        onOpLocU("loc.giants_foundry_mould_jig", "obj.bucket_water") { coolJig() }
        onOpLoc1("loc.giants_foundry_lava_pool") { soak(rising = true, fast = false) }
        onOpLoc2("loc.giants_foundry_lava_pool") { soak(rising = true, fast = true) }
        onOpLoc1("loc.giants_foundry_waterfall") { soak(rising = false, fast = false) }
        onOpLoc2("loc.giants_foundry_waterfall") { soak(rising = false, fast = true) }
        onOpLoc1("loc.giants_foundry_preform_storage") { storage() }
        onOpLoc1("loc.giants_foundry_trip_hammer") { work(Tool.TripHammer) }
        onOpLoc1("loc.giants_foundry_grindstone") { work(Tool.Grindstone) }
        onOpLoc1("loc.giants_foundry_polishing_wheel") { work(Tool.PolishingWheel) }
        for (kovac in KOVACS) onOpNpc1(kovac) { startDialogue(it.npc) { kovac() } }
    }

    private fun ProtectedAccess.enter() {
        if (player.smithingLvl < FoundryRules.MIN_SMITHING) {
            mes("You need level ${FoundryRules.MIN_SMITHING} Smithing to enter the Giants' Foundry.")
            return
        }
        PathingEntityCommon.telejump(player, collision, INSIDE)
    }

    private fun ProtectedAccess.leave() {
        PathingEntityCommon.telejump(player, collision, OUTSIDE)
    }

    private fun ProtectedAccess.fill() {
        val level = player.smithingLvl
        var added = 0
        var lacking = false
        for (metal in Metal.entries.reversed()) {
            val room = FoundryRules.CRUCIBLE_CAPACITY - player.crucibleTotal()
            if (room <= 0) break
            val held = inv.count(metal.bar)
            if (held == 0) continue
            if (level < metal.level) {
                lacking = true
                continue
            }
            val amount = minOf(room, held)
            if (invDel(inv, metal.bar, amount).failure) continue
            player.setMetalCount(metal, player.metalCount(metal) + amount)
            added += amount
        }
        syncCrucible()
        when {
            added > 0 -> mes("You add $added bars to the crucible (${player.crucibleTotal()}/28).")
            lacking -> mes("You lack the Smithing level to work the bars you are carrying.")
            player.crucibleTotal() >= FoundryRules.CRUCIBLE_CAPACITY -> mes("The crucible is full.")
            else -> mes("You have no bars to add to the crucible.")
        }
    }

    private fun ProtectedAccess.syncCrucible() {
        val total = player.crucibleTotal()
        player.crucibleState =
            when {
                total == 0 -> CRUCIBLE_EMPTY
                total >= FoundryRules.CRUCIBLE_CAPACITY -> CRUCIBLE_FULL
                else -> CRUCIBLE_PARTIAL
            }
    }

    private fun ProtectedAccess.checkCrucible() {
        val total = player.crucibleTotal()
        if (total == 0) {
            mes("The crucible is empty.")
            return
        }
        val contents =
            Metal.entries
                .filter { player.metalCount(it) > 0 }
                .joinToString { "${player.metalCount(it)} ${it.label}" }
        val score = FoundryRules.metalScore(player.crucibleCounts())
        mes("The crucible holds $total/28 bars ($contents). Metal score: $score.")
    }

    private fun ProtectedAccess.emptyCrucible() {
        var any = false
        for (metal in Metal.entries) {
            val count = player.metalCount(metal)
            if (count == 0) continue
            any = true
            player.invAddOrDrop(objs, metal.bar, count)
            player.setMetalCount(metal, 0)
        }
        syncCrucible()
        mes(if (any) "You empty the crucible." else "The crucible is empty.")
    }

    private fun ProtectedAccess.pour() {
        if (player.jigState != JIG_MOULD) {
            mes("You need to set up the moulds in the jig before pouring.")
            return
        }
        if (hasPreform() || player.preformStored == 1) {
            mes("Finish the preform you already have before casting another.")
            return
        }
        val metalScore = FoundryRules.metalScore(player.crucibleCounts())
        val picks = selectedMoulds()
        val commission = commission()
        if (picks == null || commission == null) {
            mes("You need a commission from Kovac and a full set of moulds.")
            return
        }
        val mouldScore = FoundryRules.mouldScore(commission.first, commission.second, picks)
        val start = metalScore + mouldScore
        player.preformMetalScore = metalScore
        player.preformStartQuality = start
        player.preformQuality = start
        player.preformCompletion = 0
        player.preformTemperature = 0
        repeat(FoundryRules.MAX_SECTIONS) { player.setSectionSeed(it, random.of(Tool.entries.size)) }
        for (metal in Metal.entries) player.setMetalCount(metal, 0)
        player.crucibleState = CRUCIBLE_EMPTY
        player.jigState = JIG_FILLED
        mes("You pour the molten metal into the moulds. Metal score $metalScore, mould score $mouldScore.")
    }

    private fun ProtectedAccess.selectedMoulds(): List<Mould>? {
        val forte = FoundryRules.mould(MouldType.Forte, player.selectedMouldId(MouldType.Forte))
        val blade = FoundryRules.mould(MouldType.Blade, player.selectedMouldId(MouldType.Blade))
        val tip = FoundryRules.mould(MouldType.Tip, player.selectedMouldId(MouldType.Tip))
        return if (forte == null || blade == null || tip == null) null else listOf(forte, blade, tip)
    }

    private fun ProtectedAccess.commission(): Pair<CommissionWord, CommissionStyle>? {
        val word = CommissionWord.entries.getOrNull(player.commissionWordId - 1) ?: return null
        val style = CommissionStyle.entries.getOrNull(player.commissionStyleId - 1) ?: return null
        return word to style
    }

    private suspend fun ProtectedAccess.jig() {
        when (player.jigState) {
            JIG_EMPTY -> setupMoulds()
            JIG_MOULD -> {
                val picks = selectedMoulds()
                mes("Moulds: ${picks?.joinToString { it.name }}.")
                val change =
                    choice2("Change the moulds.", true, "Leave them.", false, title = "Moulds")
                if (change) setupMoulds()
            }
            JIG_FILLED -> {
                if (wearing("obj.smithing_uniform_gloves_ice")) {
                    player.jigState = JIG_COOLED
                    takePreform()
                } else {
                    mes("The metal is molten. Use a bucket of water on the jig to cool it.")
                }
            }
            else -> takePreform()
        }
    }

    private fun ProtectedAccess.wearing(obj: String): Boolean = obj in worn

    private fun ProtectedAccess.coolJig() {
        if (player.jigState != JIG_FILLED) {
            mes("There is nothing to cool.")
            return
        }
        if (invDel(inv, "obj.bucket_water", 1).failure) return
        invAdd(inv, "obj.bucket_empty", 1)
        player.jigState = JIG_COOLED
        mes("You douse the jig and the metal sets.")
    }

    private fun ProtectedAccess.takePreform() {
        if (!inv.hasFreeSpace()) {
            mes("You don't have enough inventory space.")
            return
        }
        invAdd(inv, "obj.giants_foundry_preform", 1)
        player.jigState = JIG_EMPTY
        player.preformTemperature = 0
        player.preformCompletion = 0
        mes("You lift the preform out of the jig. It needs heating before it can be worked.")
    }

    private suspend fun ProtectedAccess.setupMoulds() {
        val commission = commission()
        if (commission == null) {
            mes("Speak to Kovac for a commission first.")
            return
        }
        val chosen = ArrayList<Mould>()
        for (type in MouldType.entries) {
            val options = FoundryRules.moulds(type).filter { player.hasMould(it) }
            val labels = options.map { "${it.name} (+${it.score(commission.first, commission.second)})" }
            val index = pick("Choose a ${type.name.lowercase()} mould", labels) ?: return
            chosen += options[index]
        }
        for (mould in chosen) player.selectMould(mould.type, mould.id)
        player.jigState = JIG_MOULD
        val score = FoundryRules.mouldScore(commission.first, commission.second, chosen)
        mes("The moulds are set. Mould score: $score.")
    }

    private fun ProtectedAccess.hasPreform(): Boolean = inv.contains("obj.giants_foundry_preform")

    private fun ProtectedAccess.storage() {
        if (player.preformStored == 1) {
            if (invAdd(inv, "obj.giants_foundry_preform", 1).failure) {
                mes("You don't have enough inventory space.")
                return
            }
            player.preformStored = 0
            mes("You take the preform from storage.")
        } else if (hasPreform()) {
            invDel(inv, "obj.giants_foundry_preform", 1)
            player.preformStored = 1
            mes("You place the preform in storage. Its heat and progress are held until you take it back.")
        } else {
            mes("You have no preform to store.")
        }
    }

    private suspend fun ProtectedAccess.soak(rising: Boolean, fast: Boolean) {
        if (!hasPreform()) {
            mes("You need a preform for that.")
            return
        }
        var temperature = player.preformTemperature.toDouble()
        var steps = 0
        mes(if (rising) "You hold the preform over the lava." else "You hold the preform in the water.")
        while (if (rising) temperature < 100 else temperature > 0) {
            delay(if (fast) 1 else 2)
            if (!hasPreform()) return
            val ramp = (steps * RAMP_STEP).coerceAtMost(MAX_RAMP)
            val change = (if (fast) FAST_RATE else SLOW_RATE) * (1 + ramp)
            temperature = (temperature + if (rising) change else -change).coerceIn(0.0, 100.0)
            player.preformTemperature = temperature.roundToInt()
            steps++
        }
        mes(if (rising) "The preform is white hot." else "The preform is cold.")
    }

    private suspend fun ProtectedAccess.work(tool: Tool) {
        if (!hasPreform()) {
            mes("You need a preform to work on.")
            return
        }
        val metalScore = player.preformMetalScore
        val sections = FoundryRules.sections(metalScore)
        var temperature = player.preformTemperature.toDouble()
        var ticks = 0
        while (player.preformCompletion < 100) {
            val completion = player.preformCompletion
            val wanted = FoundryRules.toolFor(player.sectionSeed(FoundryRules.sectionOf(completion, sections)))
            if (tool != wanted) {
                spoil("This stage needs the ${wanted.label}, not the ${tool.label}.")
                return
            }
            val required = FoundryRules.requiredHeat(tool)
            if (FoundryRules.heatOf(temperature, metalScore) != required) {
                spoil("The ${tool.label} needs the preform ${required.label}.")
                return
            }
            anim(tool.animation)
            delay(1)
            if (!hasPreform()) return
            if (++ticks % tool.interval != 0) continue
            player.preformCompletion = (completion + tool.progress).coerceAtMost(100)
            temperature = (temperature + tool.heat).coerceIn(0.0, 100.0)
            player.preformTemperature = temperature.roundToInt()
            statAdvance("stat.smithing", tool.xp)
        }
        mes("The sword is fully worked. Hand it in to Kovac.")
    }

    private fun ProtectedAccess.spoil(reason: String) {
        player.preformQuality = (player.preformQuality - FoundryRules.ERROR_PENALTY).coerceAtLeast(0)
        mes("$reason The sword loses quality (now ${player.preformQuality}).")
    }

    private suspend fun ProtectedAccess.pick(title: String, labels: List<String>): Int? {
        var page = 0
        while (true) {
            val start = page * PAGE
            val entries = ArrayList<Pair<String, Int>>()
            labels.drop(start).take(PAGE).forEachIndexed { i, label -> entries += label to start + i }
            if (start + PAGE < labels.size) entries += "More options" to MORE
            entries += "Cancel" to CANCEL
            when (val chosen = choose(title, entries)) {
                CANCEL -> return null
                MORE -> page++
                else -> return chosen
            }
        }
    }

    private suspend fun ProtectedAccess.choose(title: String, e: List<Pair<String, Int>>): Int =
        when (e.size) {
            2 -> choice2(e[0].first, e[0].second, e[1].first, e[1].second, title = title)
            3 ->
                choice3(
                    e[0].first, e[0].second,
                    e[1].first, e[1].second,
                    e[2].first, e[2].second,
                    title = title,
                )
            4 ->
                choice4(
                    e[0].first, e[0].second,
                    e[1].first, e[1].second,
                    e[2].first, e[2].second,
                    e[3].first, e[3].second,
                    title = title,
                )
            else ->
                choice5(
                    e[0].first, e[0].second,
                    e[1].first, e[1].second,
                    e[2].first, e[2].second,
                    e[3].first, e[3].second,
                    e[4].first, e[4].second,
                    title = title,
                )
        }

    private suspend fun Dialogue.kovac() {
        chatNpc(neutral, "Foreman Kovac. Mind the heat. What do you need?")
        val option =
            choice5(
                "Ask for a commission.", 0,
                "Hand in a sword.", 1,
                "Buy moulds.", 2,
                "Rewards.", 3,
                "Nothing.", 4,
                title = "Kovac",
            )
        when (option) {
            0 -> commissionTalk()
            1 -> handIn()
            2 -> mouldShop()
            3 -> rewardShop()
        }
    }

    private suspend fun Dialogue.commissionTalk() {
        val player = access.player
        if (player.commissionWordId == 0 || random.of(2) == 0) newCommission()
        val (word, style) = access.commission()!!
        chatNpc(
            neutral,
            "I need a ${word.label} ${style.label} sword. Score the moulds against those words.",
        )
    }

    private fun Dialogue.newCommission() {
        access.player.commissionWordId = random.of(CommissionWord.entries.size) + 1
        access.player.commissionStyleId = random.of(CommissionStyle.entries.size) + 1
    }

    private suspend fun Dialogue.handIn() {
        val player = access.player
        if (!access.hasPreform()) {
            chatNpc(neutral, "You have no sword for me.")
            return
        }
        if (player.preformCompletion < 100) {
            chatNpc(neutral, "That sword isn't finished yet.")
            return
        }
        val quality = player.preformQuality
        access.invDel(access.inv, "obj.giants_foundry_preform", 1)
        access.statAdvance("stat.smithing", FoundryRules.handInXp(quality).toDouble())
        player.invAddOrDrop(objs, "obj.coins", FoundryRules.handInCoins(quality))
        player.reputation += FoundryRules.reputation(quality)
        player.preformCompletion = 0
        newCommission()
        chatNpc(
            happy,
            "Quality $quality. Fine work. You have ${player.reputation} reputation with the foundry.",
        )
    }

    private suspend fun Dialogue.mouldShop() {
        val player = access.player
        val stock = FoundryRules.moulds.filter { it.purchasable && !player.hasMould(it) }
        if (stock.isEmpty()) {
            chatNpc(happy, "You've bought every mould I have.")
            return
        }
        val labels = stock.map { "${it.name} - ${it.cost}" }
        val index = access.pick("Moulds (${player.reputation} reputation)", labels) ?: return
        val mould = stock[index]
        when {
            player.smithingLvl < mould.level ->
                chatNpc(neutral, "You need level ${mould.level} Smithing for that one.")
            player.reputation < mould.cost -> chatNpc(neutral, "You haven't earned enough reputation.")
            else -> {
                player.reputation -= mould.cost
                player.unlockMould(mould)
                chatNpc(happy, "The ${mould.name} mould is yours.")
            }
        }
    }

    private suspend fun Dialogue.rewardShop() {
        val player = access.player
        val labels = REWARDS.map { "${it.label} - ${it.cost}" }
        val index = access.pick("Rewards (${player.reputation} reputation)", labels) ?: return
        val reward = REWARDS[index]
        if (player.reputation < reward.cost) {
            chatNpc(neutral, "You haven't earned enough reputation.")
            return
        }
        if (access.invAdd(access.inv, reward.obj, reward.count).failure) {
            chatNpc(neutral, "Make some room in your pack first.")
            return
        }
        player.reputation -= reward.cost
        chatNpc(happy, "Here you go.")
    }

    private class Reward(val label: String, val cost: Int, val obj: String, val count: Int = 1)

    private companion object {
        const val CRUCIBLE_EMPTY = 0
        const val CRUCIBLE_PARTIAL = 1
        const val CRUCIBLE_FULL = 2
        const val JIG_EMPTY = 0
        const val JIG_MOULD = 1
        const val JIG_FILLED = 2
        const val JIG_COOLED = 3
        const val PAGE = 3
        const val MORE = -2
        const val CANCEL = -1
        const val SLOW_RATE = 1.0
        const val FAST_RATE = 2.0
        const val RAMP_STEP = 0.25
        const val MAX_RAMP = 3.0

        val INSIDE = CoordGrid(3365, 11483, 0)
        val OUTSIDE = CoordGrid(3359, 3149, 0)

        val KOVACS =
            listOf(
                "npc.giants_foundry_kovac_multi",
                "npc.giants_foundry_kovac",
                "npc.giants_foundry_kovac_1op",
            )

        val REWARDS =
            listOf(
                Reward("Ore pack", 200, "obj.giants_foundry_ore_pack"),
                Reward("Kovac's grog", 300, "obj.kovacs_grog"),
                Reward("Smithing catalyst", 15, "obj.smithing_catalyst"),
                Reward("Smiths' boots", 3500, "obj.smithing_uniform_boots"),
                Reward("Smiths' gloves", 3500, "obj.smithing_uniform_gloves"),
                Reward("Smiths' tunic", 4000, "obj.smithing_uniform_torso"),
                Reward("Smiths' trousers", 4000, "obj.smithing_uniform_legs"),
                Reward("Colossal blade", 5000, "obj.giants_foundry_colossal_blade"),
            )
    }
}

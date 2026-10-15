package org.rsmod.content.minigames.barbassault

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BaGambleScript
@Inject
constructor(
    private val objs: ObjRepository,
    private val random: GameRandom,
    private val pets: PetRewards,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(COMMANDER) { talk(it.npc) }
        onOpNpc3(COMMANDER) { rewards(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            chatNpc(neutral, "Your points: ${summary(player)}. Care to gamble some of them?")
            val tier =
                choice4(
                    GambleTier.Low.label,
                    GambleTier.Low,
                    GambleTier.Medium.label,
                    GambleTier.Medium,
                    GambleTier.High.label,
                    GambleTier.High,
                    "Not now",
                    null,
                ) ?: return@startDialogue
            val role = chooseRole(player)
            val problem =
                BaGamble.available(
                    tier,
                    BaPoints.get(player, role),
                    BaPoints.gambleProgress(player),
                    BaPoints.queenKills(player),
                )
            if (problem != null) {
                chatNpc(neutral, problem)
                return@startDialogue
            }
            if (!BaPoints.spend(player, role, tier.cost)) return@startDialogue
            BaPoints.consumeGamble(player, tier)
            val result = BaGamble.roll(tier) { random.of(it) }
            player.invAddOrDrop(objs, symbolOf(result.item), result.quantity)
            val name = ServerCacheManager.getItem(result.item.obj.asRSCM(RSCMType.OBJ))?.name ?: "reward"
            player.mes("You receive ${result.quantity} x $name.")
            if (tier == GambleTier.High && random.of(BaGear.PET_ONE_IN) == 0) pets.give(player, BaGear.PET)
        }
    }

    private suspend fun ProtectedAccess.rewards(npc: Npc) {
        startDialogue(npc) {
            chatNpc(neutral, "Your points: ${summary(player)}. Queen kills: ${BaPoints.queenKills(player)}.")
            val gear =
                choice5(
                    "Hats",
                    Menu.Hats,
                    "Fighter torso",
                    Menu.Item(BaGear.Torso),
                    "Penance skirt",
                    Menu.Item(BaGear.Skirt),
                    "Gloves or boots",
                    Menu.Basics,
                    "Granite body",
                    Menu.Granite,
                )
            when (gear) {
                Menu.Hats -> hats()
                is Menu.Item -> buy(gear.gear)
                Menu.Basics -> basics()
                Menu.Granite -> granite()
            }
        }
    }

    private suspend fun Dialogue.hats() {
        val hat =
            choice4(
                "Fighter hat",
                BaGear.FighterHat,
                "Ranger hat",
                BaGear.RangerHat,
                "Healer hat",
                BaGear.HealerHat,
                "Runner hat",
                BaGear.RunnerHat,
            )
        buy(hat)
    }

    private suspend fun Dialogue.basics() {
        val item = choice2("Penance gloves", BaGear.Gloves, "Runner boots", BaGear.Boots)
        buy(item)
    }

    private suspend fun Dialogue.buy(gear: BaGear) {
        val role = chooseRole(player)
        val problem = gear.blocked(BaPoints.get(player, role), BaPoints.queenKills(player))
        if (problem != null) {
            chatNpc(neutral, problem)
            return
        }
        if (!BaPoints.spend(player, role, gear.cost)) return
        player.invAddOrDrop(objs, gear.obj, 1)
        player.mes("You buy ${gear.label.substringBefore(" (")}.")
    }

    private suspend fun Dialogue.granite() {
        val coins = player.inv.count("obj.coins")
        val problem = BaGear.graniteBodyBlocked(coins, BaPoints.queenKills(player))
        if (problem != null) {
            chatNpc(neutral, problem)
            return
        }
        if (!player.invDel(player.inv, "obj.coins", BaGear.GRANITE_BODY_COINS).success) return
        player.invAddOrDrop(objs, BaGear.GRANITE_BODY, 1)
        player.mes("You buy a granite body.")
    }

    private suspend fun Dialogue.chooseRole(player: Player): BaRole =
        choice4(
            "Attacker (${BaPoints.get(player, BaRole.Attacker)})",
            BaRole.Attacker,
            "Defender (${BaPoints.get(player, BaRole.Defender)})",
            BaRole.Defender,
            "Healer (${BaPoints.get(player, BaRole.Healer)})",
            BaRole.Healer,
            "Collector (${BaPoints.get(player, BaRole.Collector)})",
            BaRole.Collector,
            title = "Pay with which role?",
        )

    private fun summary(player: Player): String =
        BaRole.entries.joinToString(", ") { "${it.label} ${BaPoints.get(player, it)}" }

    private fun symbolOf(item: GambleItem): String {
        if (!item.noted) return item.obj
        val base = ServerCacheManager.getItem(item.obj.asRSCM(RSCMType.OBJ)) ?: return item.obj
        return if (base.certlink > 0) RSCM.getReverseMapping(RSCMType.OBJ, base.certlink) else item.obj
    }

    private sealed interface Menu {
        data object Hats : Menu

        data object Basics : Menu

        data object Granite : Menu

        data class Item(val gear: BaGear) : Menu
    }

    companion object {
        const val COMMANDER = "npc.barbassault_barbarian_commander"
    }
}

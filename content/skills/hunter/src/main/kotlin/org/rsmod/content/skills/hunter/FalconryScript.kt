package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hunterLvl
import org.rsmod.api.player.stat.statRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FalconryScript
@Inject
constructor(private val npcRepo: NpcRepository, private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        for (kebbit in Falconry.kebbits) {
            onOpNpc1(kebbit.npc) { hunt(it.npc, kebbit) }
        }
        for (matthias in MATTHIAS) {
            onOpNpc1(matthias) { talk(it.npc) }
            onOpNpc3(matthias) { buyGlove() }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            chatNpc(
                neutral,
                "I rent out falcons for hunting kebbits. A falconer's glove is ${Falconry.GLOVE_PRICE} " +
                    "coins and you need ${Falconry.GLOVE_LEVEL} Hunter. Want one?",
            )
            if (choice2("Yes, please.", true, "No, thanks.", false)) {
                access.buyGlove()
            }
        }
    }

    private fun ProtectedAccess.buyGlove() {
        if (player.hunterLvl < Falconry.GLOVE_LEVEL) {
            mes("You need a Hunter level of ${Falconry.GLOVE_LEVEL} to use a falcon.")
            return
        }
        if (hasGlove()) {
            mes("You already have a falconer's glove.")
            return
        }
        val result =
            player.invTransaction(inv) {
                val into = select(inv)
                delete {
                    this.from = into
                    this.obj = Falconry.COINS.asRSCM(RSCMType.OBJ)
                    this.strictCount = Falconry.GLOVE_PRICE
                }
                insert {
                    this.into = into
                    this.obj = Falconry.GLOVE.asRSCM(RSCMType.OBJ)
                }
            }
        if (result.failure) {
            mes("You need ${Falconry.GLOVE_PRICE} coins and a free inventory slot.")
            return
        }
        mes("Matthias hands you a falconer's glove.")
    }

    private suspend fun ProtectedAccess.hunt(npc: Npc, kebbit: Kebbit) {
        if (player.hunterLvl < kebbit.level) {
            mes("You need a Hunter level of ${kebbit.level} to catch a ${kebbit.name}.")
            return
        }
        if (!wearingGlove()) {
            mes("You need to wear a falconer's glove to hunt with a falcon.")
            return
        }
        if (!npc.isVisible) {
            return
        }
        mes("You send the falcon after the ${kebbit.name}.")
        anim(THROW_ANIM)
        delay(HUNT_TICKS)
        if (!npc.isVisible) {
            return
        }
        val caught =
            player.statRandom(random, STAT, kebbit.catchLow, kebbit.catchHigh, invisibleBoost = 0)
        if (!caught) {
            mes("The falcon fails to catch the ${kebbit.name}.")
            return
        }
        val result =
            player.invTransaction(inv) {
                val into = select(inv)
                for (obj in listOf(kebbit.fur, Falconry.BONES)) {
                    insert {
                        this.into = into
                        this.obj = obj.asRSCM(RSCMType.OBJ)
                    }
                }
            }
        if (result.failure) {
            mes("You don't have enough inventory space to do that.")
            return
        }
        npcRepo.hide(npc, RESPAWN_TICKS)
        statAdvance(STAT, kebbit.xp)
        mes("The falcon catches the ${kebbit.name} and brings it back to you.")
    }

    private fun ProtectedAccess.wearingGlove(): Boolean {
        val worn = player.worn[Wearpos.Hands.slot]
        return worn != null && worn.isType(Falconry.GLOVE)
    }

    private fun ProtectedAccess.hasGlove(): Boolean = inv.contains(Falconry.GLOVE) || wearingGlove()

    private companion object {
        const val STAT = "stat.hunter"
        const val THROW_ANIM = "seq.human_pickupfloor"
        const val HUNT_TICKS = 3
        const val RESPAWN_TICKS = 40
        val MATTHIAS = listOf("npc.hunting_npc_falconer", "npc.hunting_npc_falconer_nobird")
    }
}

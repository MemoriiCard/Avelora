package org.rsmod.content.skills.hunter

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hunterLvl
import org.rsmod.api.player.stat.statRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ImplingScript
@Inject
constructor(private val npcRepo: NpcRepository, private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        for (impling in Implings.all + Implings.butterflies) {
            for (npc in impling.npcs) {
                onOpNpc1(npc) { catch(it.npc, impling) }
            }
        }
    }

    private suspend fun ProtectedAccess.catch(npc: Npc, impling: Impling) {
        if (player.hunterLvl < impling.level) {
            mes("You need a Hunter level of ${impling.level} to catch that ${impling.noun}.")
            return
        }
        if (!hasNet()) {
            mes("You need a butterfly net to catch that.")
            return
        }
        if (!inv.contains(impling.emptyJar)) {
            mes("You need an empty jar to catch that ${impling.noun}.")
            return
        }
        if (!npc.isVisible) {
            return
        }
        anim(NET_ANIM)
        delay(CATCH_TICKS)
        if (!npc.isVisible) {
            return
        }
        val caught =
            player.statRandom(random, STAT, impling.catchLow, impling.catchHigh, invisibleBoost = 0)
        if (!caught) {
            mes("You fail to catch the ${impling.noun}.")
            return
        }
        if (invDel(inv, impling.emptyJar).failure) {
            return
        }
        invAdd(inv, impling.jar)
        npcRepo.hide(npc, RESPAWN_TICKS)
        statAdvance(STAT, impling.xp)
        mes("You manage to catch the ${impling.noun} and squeeze it into a jar.")
    }

    private fun ProtectedAccess.hasNet(): Boolean {
        val weapon = player.worn[Wearpos.RightHand.slot]
        return inv.contains(Implings.NET) ||
            inv.contains(Implings.MAGIC_NET) ||
            (weapon != null && (weapon.isType(Implings.NET) || weapon.isType(Implings.MAGIC_NET)))
    }

    private companion object {
        const val STAT = "stat.hunter"
        const val NET_ANIM = "seq.human_butterflynet_swing"
        const val CATCH_TICKS = 2
        const val RESPAWN_TICKS = 40
    }
}

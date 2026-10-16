package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.content.skills.agility.courses.BarbarianOutpostCourse
import org.rsmod.content.skills.agility.courses.wildernessTag
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AgilityExtrasScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.agility_obstical_pipe_barbarian") { barbarianPipe() }
        onOpLoc1("loc.wildy_agility_pillar") { tagDispenser() }
        onOpLoc2("loc.wildy_agility_pillar") { mes("The dispenser takes no payment here.") }
        onOpLoc3("loc.wildy_agility_pillar") { redeemTokens() }
    }

    private suspend fun ProtectedAccess.barbarianPipe() {
        if (player.agilityLvl < BarbarianOutpostCourse.PIPE_LEVEL) {
            mes("You need an Agility level of ${BarbarianOutpostCourse.PIPE_LEVEL} to squeeze through.")
            return
        }
        val north = coords.z >= BarbarianOutpostCourse.PIPE_NORTH.z - 1
        val start = if (north) BarbarianOutpostCourse.PIPE_NORTH else BarbarianOutpostCourse.PIPE_SOUTH
        val end = if (north) BarbarianOutpostCourse.PIPE_SOUTH else BarbarianOutpostCourse.PIPE_NORTH
        stepTo(start)
        balanceWalk(end, "seq.human_pipesqueeze", "seq.human_pipesqueeze_ready")
    }

    private fun ProtectedAccess.tagDispenser() {
        if (player.wildernessTag == 0) {
            mes("Complete a lap of the course before tagging the dispenser.")
            return
        }
        player.wildernessTag = 0
        if (player.invAdd(player.inv, TOKEN, 1).success) {
            mes("The dispenser gives you a Wilderness agility ticket.")
        } else {
            player.wildernessTag = 1
            mes("You don't have enough inventory space.")
        }
    }

    private fun ProtectedAccess.redeemTokens() {
        val count = player.inv.count(TOKEN)
        if (count == 0) {
            mes("You have no Wilderness agility tickets to redeem.")
            return
        }
        if (!player.invDel(player.inv, TOKEN, count).success) return
        statAdvance("stat.agility", tokenXp(count))
        mes("You redeem $count ticket${if (count == 1) "" else "s"} for Agility experience.")
    }

    companion object {
        const val TOKEN = "obj.wildy_agility_token"
        const val TOKEN_XP = 200.0
        const val BULK_THRESHOLD = 101
        const val BULK_BONUS_PERCENT = 15

        fun tokenXp(count: Int): Double {
            val base = count * TOKEN_XP
            return if (count >= BULK_THRESHOLD) base * (100 + BULK_BONUS_PERCENT) / 100 else base
        }
    }
}

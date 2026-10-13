package org.rsmod.content.minigames.colosseum

import jakarta.inject.Inject
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ColosseumMinimus
@Inject
internal constructor(private val instances: InstanceManager, private val runs: ColosseumRuns) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(MINIMUS_NPC) { talk() }
    }

    private suspend fun ProtectedAccess.talk() {
        val session = instances.sessionForPlayer(player)
        val run = session?.let { runs.runFor(it) }
        if (run == null) {
            player.mes("Minimus: The arena is open to those with the courage to enter.")
            return
        }
        when (run.state) {
            RunState.Fighting -> player.mes("Minimus: Finish the wave first.")
            RunState.Done -> Unit
            RunState.Waiting -> if (run.cleared == 0) first(run) else between(run)
        }
    }

    private suspend fun ProtectedAccess.first(run: ColosseumRun) {
        val begin =
            choice2(
                "Begin the first wave.", true,
                "Leave the arena.", false,
                title = "Minimus: Ready to fight for glory?",
            )
        if (begin) runs.beginWave(run, null) else runs.claim(run)
    }

    private suspend fun ProtectedAccess.between(run: ColosseumRun) {
        val next = run.nextWave
        val action =
            choice2(
                "Fight wave $next.", true,
                "Claim rewards and leave.", false,
                title = "Wave ${run.cleared} cleared. Glory ${run.glory}.",
            )
        if (!action) {
            runs.claim(run)
            return
        }
        runs.beginWave(run, pickModifier(run))
    }

    private suspend fun ProtectedAccess.pickModifier(run: ColosseumRun): ColosseumModifier? {
        val offers = runs.offers(run)
        val title = "Choose a modifier for the next wave."
        return when (offers.size) {
            0 -> null
            1 -> choice2(offers[0].title, offers[0], SKIP, null, title = title)
            2 -> choice3(offers[0].title, offers[0], offers[1].title, offers[1], SKIP, null, title = title)
            else ->
                choice4(
                    offers[0].title, offers[0],
                    offers[1].title, offers[1],
                    offers[2].title, offers[2],
                    SKIP, null,
                    title = title,
                )
        }
    }

    private companion object {
        const val SKIP = "No modifier."
    }
}

package org.rsmod.content.bosses.nex

import jakarta.inject.Singleton
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
class NexFight {
    internal var nex: Npc? = null
    internal var state: State = State.Absent
    internal var phase: NexPhase = NexPhase.Smoke
    internal var mageTurn: Boolean = false
    internal val mages = mutableMapOf<NexMage, Npc>()
    internal var siphonEndsAt: Int = 0
    internal val reavers = mutableListOf<Npc>()
    internal val infected = mutableSetOf<Player>()
    internal val lockedIn = mutableSetOf<Player>()
    internal var emptyTicks: Int = 0
    internal var retargetAt: Int = 0

    internal val inProgress: Boolean
        get() = state == State.Intro || state == State.Fighting

    internal fun isShielded(mage: Npc): Boolean {
        val current = phase.mage ?: return true
        return !mageTurn || mages[current] !== mage
    }

    internal fun reset(npc: Npc) {
        nex = npc
        state = State.Idle
        phase = NexPhase.Smoke
        mageTurn = false
        mages.clear()
        siphonEndsAt = 0
        reavers.clear()
        infected.clear()
        emptyTicks = 0
        retargetAt = 0
    }

    internal enum class State {
        Absent,
        Idle,
        Intro,
        Fighting,
        Dead,
    }
}

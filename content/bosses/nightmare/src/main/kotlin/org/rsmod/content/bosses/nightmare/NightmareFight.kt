package org.rsmod.content.bosses.nightmare

import jakarta.inject.Singleton
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
class NightmareFight {
    internal var nightmare: Npc? = null
    internal var entry: Npc? = null
    internal val totems = mutableMapOf<NightmareTotem, Npc>()
    internal val chargedTotems = mutableSetOf<NightmareTotem>()
    internal var state: State = State.Absent
    internal var phase: NightmarePhase = NightmarePhase.One
    internal var shield: Int = 0
    internal var lobbyEndsAt: Int = 0
    internal var emptyTicks: Int = 0
    internal var retargetAt: Int = 0
    internal val bound = mutableMapOf<Player, List<Npc>>()
    internal val spores = mutableMapOf<CoordGrid, Int>()
    internal val drowsyUntil = mutableMapOf<Player, Int>()
    internal val sleepwalkers = mutableListOf<Npc>()
    internal var sleepwalkersSummoned: Int = 0
    internal var sleepwalkersAbsorbed: Int = 0
    internal var sleepwalkersEndAt: Int = 0

    internal val inProgress: Boolean
        get() = state in FIGHTING

    internal fun isTotemShut(totem: Npc): Boolean {
        val entry = totems.entries.firstOrNull { it.value === totem } ?: return true
        return state != State.Totems || entry.key in chargedTotems
    }

    internal fun clearMechanics() {
        chargedTotems.clear()
        bound.clear()
        spores.clear()
        drowsyUntil.clear()
        sleepwalkers.clear()
        sleepwalkersSummoned = 0
        sleepwalkersAbsorbed = 0
        sleepwalkersEndAt = 0
    }

    internal enum class State {
        Absent,
        Idle,
        Lobby,
        Shield,
        Vulnerable,
        Totems,
        Sleepwalkers,
        Blast,
        Dying,
        Dead,
    }

    internal companion object {
        val FIGHTING = setOf(State.Shield, State.Vulnerable, State.Totems, State.Sleepwalkers, State.Blast, State.Dying)

        fun shieldFor(players: Int): Int = SHIELD_BASE + SHIELD_PER_EXTRA_PLAYER * (players - 1).coerceAtLeast(0)

        const val SHIELD_BASE = 400
        const val SHIELD_PER_EXTRA_PLAYER = 200
    }
}

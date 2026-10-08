package org.rsmod.content.raids.toa.party

import org.rsmod.content.raids.toa.invocation.ToaInvocation
import org.rsmod.content.raids.toa.invocation.ToaInvocations
import org.rsmod.game.entity.Player

class ToaParty(val id: Int, leader: Player) {
    var leader: Player = leader
        internal set

    internal val members: MutableList<Player> = mutableListOf(leader)
    internal var invocations: Set<ToaInvocation> = emptySet()
    internal var minCombat: Int = 0
    internal var started: Boolean = false

    val size: Int
        get() = members.size

    val raidLevel: Int
        get() = ToaInvocations.raidLevel(invocations)

    val mode: ToaMode
        get() = ToaMode.of(raidLevel)

    val name: String
        get() = "${leader.displayName}'s party"

    fun isLeader(player: Player): Boolean = leader === player

    operator fun contains(player: Player): Boolean = members.any { it === player }

    fun snapshot(): List<Player> = members.toList()
}

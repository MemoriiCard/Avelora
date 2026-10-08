package org.rsmod.content.raids.tob.party

import org.rsmod.game.entity.Player

class TobParty(val id: Int, leader: Player) {
    var leader: Player = leader
        internal set

    internal val members: MutableList<Player> = mutableListOf(leader)
    internal var mode: TobMode = TobMode.Normal
    internal var minCombat: Int = 0
    internal var preferredSize: Int = TobScaling.MAX_PARTY
    internal var started: Boolean = false

    val size: Int
        get() = members.size

    val name: String
        get() = "${leader.displayName}'s party"

    fun isLeader(player: Player): Boolean = leader === player

    operator fun contains(player: Player): Boolean = members.any { it === player }

    fun snapshot(): List<Player> = members.toList()
}

package org.rsmod.content.raids.cox.party

import org.rsmod.game.entity.Player

class CoxParty(val id: Int, leader: Player) {
    var leader: Player = leader
        internal set

    internal val members: MutableList<Player> = mutableListOf(leader)
    internal var preferredSize: Int = 0
    internal var minCombat: Int = 0
    internal var minSkillTotal: Int = 0
    internal var scaling: Int = 0
    internal var challengeMode: Boolean = false
    internal var mapPool: CoxMapPool = CoxMapPool.Full
    internal var advertisedAt: Int = -1
    internal var progress: CoxProgress = CoxProgress.Lobby

    val size: Int
        get() = members.size

    val isAdvertised: Boolean
        get() = advertisedAt >= 0

    val inRaid: Boolean
        get() = progress != CoxProgress.Lobby

    val name: String
        get() = "${leader.displayName}'s party"

    fun isLeader(player: Player): Boolean = leader === player

    operator fun contains(player: Player): Boolean = members.any { it === player }

    fun canToggleChallengeMode(): Boolean = mapPool == CoxMapPool.Full && !inRaid
}

enum class CoxMapPool(val id: Int, val label: String) {
    Small(0, "Small"),
    Large(1, "Large"),
    Full(2, "Full");

    companion object {
        operator fun get(id: Int): CoxMapPool = entries.firstOrNull { it.id == id } ?: Full
    }
}

enum class CoxProgress(val clientId: Int) {
    Lobby(0),
    Upper(1),
    Middle(2),
    Lower(3),
    Olm(4),
    Collapsing(5),
}

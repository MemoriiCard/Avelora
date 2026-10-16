package org.rsmod.content.raids.cox.party

import jakarta.inject.Singleton
import org.rsmod.game.entity.Player

@Singleton
class CoxParties {
    private val parties = linkedMapOf<Int, CoxParty>()
    private var nextId = 0

    val all: Collection<CoxParty>
        get() = parties.values

    fun advertised(): List<CoxParty> = parties.values.filter { it.isAdvertised && !it.inRaid }

    operator fun get(id: Int): CoxParty? = parties[id]

    fun of(player: Player): CoxParty? = parties.values.firstOrNull { player in it }

    fun create(leader: Player): CoxParty {
        check(of(leader) == null) { "Player already in a party: $leader" }
        val party = CoxParty(allocateId(), leader)
        parties[party.id] = party
        return party
    }

    fun join(party: CoxParty, player: Player): JoinResult {
        if (of(player) != null) return JoinResult.AlreadyInParty
        if (party.inRaid) return JoinResult.AlreadyStarted
        if (party.size >= MAX_PARTY_SIZE) return JoinResult.Full
        party.members += player
        return JoinResult.Joined
    }

    /** Removes [player] from their party, promoting the next member or disbanding when empty. */
    fun leave(player: Player): CoxParty? {
        val party = of(player) ?: return null
        party.members.removeAll { it === player }
        if (party.members.isEmpty()) {
            parties.remove(party.id)
        } else if (party.leader === player) {
            party.leader = party.members.first()
        }
        return party
    }

    fun disband(party: CoxParty): List<Player> {
        parties.remove(party.id)
        val members = party.members.toList()
        party.members.clear()
        return members
    }

    private fun allocateId(): Int {
        while (parties.containsKey(nextId)) {
            nextId = (nextId + 1) and MAX_PARTY_ID
        }
        return nextId.also { nextId = (nextId + 1) and MAX_PARTY_ID }
    }

    enum class JoinResult {
        Joined,
        AlreadyInParty,
        AlreadyStarted,
        Full,
    }

    companion object {
        const val MAX_PARTY_SIZE = 100
        private const val MAX_PARTY_ID = 0x7FFF
    }
}

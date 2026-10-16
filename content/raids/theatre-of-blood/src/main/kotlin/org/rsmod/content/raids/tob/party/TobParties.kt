package org.rsmod.content.raids.tob.party

import jakarta.inject.Singleton
import org.rsmod.game.entity.Player

@Singleton
class TobParties {
    private val parties = linkedMapOf<Int, TobParty>()
    private var nextId = 0

    val all: Collection<TobParty>
        get() = parties.values

    fun open(): List<TobParty> =
        parties.values.filter { !it.started && it.size < TobScaling.MAX_PARTY }

    operator fun get(id: Int): TobParty? = parties[id]

    fun of(player: Player): TobParty? = parties.values.firstOrNull { player in it }

    fun create(leader: Player): TobParty {
        check(of(leader) == null) { "Player already in a party: $leader" }
        val party = TobParty(nextId++, leader)
        parties[party.id] = party
        return party
    }

    fun join(party: TobParty, player: Player): JoinResult =
        when {
            of(player) != null -> JoinResult.AlreadyInParty
            party.started -> JoinResult.AlreadyStarted
            party.size >= TobScaling.MAX_PARTY -> JoinResult.Full
            else -> {
                party.members += player
                JoinResult.Joined
            }
        }

    fun leave(player: Player): TobParty? {
        val party = of(player) ?: return null
        party.members.removeAll { it === player }
        if (party.members.isEmpty()) {
            parties.remove(party.id)
        } else if (party.leader === player) {
            party.leader = party.members.first()
        }
        return party
    }

    fun disband(party: TobParty): List<Player> {
        parties.remove(party.id)
        val members = party.snapshot()
        party.members.clear()
        return members
    }

    enum class JoinResult {
        Joined,
        AlreadyInParty,
        AlreadyStarted,
        Full,
    }
}

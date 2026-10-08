package org.rsmod.content.raids.toa.party

import jakarta.inject.Singleton
import org.rsmod.game.entity.Player

@Singleton
class ToaParties {
    private val parties = linkedMapOf<Int, ToaParty>()
    private var nextId = 0

    val all: Collection<ToaParty>
        get() = parties.values

    fun open(): List<ToaParty> =
        parties.values.filter { !it.started && it.size < ToaScaling.MAX_PARTY }

    operator fun get(id: Int): ToaParty? = parties[id]

    fun of(player: Player): ToaParty? = parties.values.firstOrNull { player in it }

    fun create(leader: Player): ToaParty {
        check(of(leader) == null) { "Player already in a party: $leader" }
        val party = ToaParty(nextId++, leader)
        parties[party.id] = party
        return party
    }

    fun join(party: ToaParty, player: Player): JoinResult =
        when {
            of(player) != null -> JoinResult.AlreadyInParty
            party.started -> JoinResult.AlreadyStarted
            party.size >= ToaScaling.MAX_PARTY -> JoinResult.Full
            else -> {
                party.members += player
                JoinResult.Joined
            }
        }

    fun leave(player: Player): ToaParty? {
        val party = of(player) ?: return null
        party.members.removeAll { it === player }
        if (party.members.isEmpty()) {
            parties.remove(party.id)
        } else if (party.leader === player) {
            party.leader = party.members.first()
        }
        return party
    }

    fun disband(party: ToaParty): List<Player> {
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

package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.map.zone.ZoneKey

@Singleton
internal class KrakenWhirlpools
@Inject
constructor(private val deps: BossDeps, private val aiPlayerInteractions: AiPlayerInteractions) {
    private val disturbed: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap())

    fun onIncomingHit(npc: Npc, hit: HitBuilder) {
        val attacker = attacker(hit)
        when (npc.visType.internalName) {
            KRAKEN_WHIRLPOOL ->
                if (attacker != null && tentaclesDisturbed(npc)) emerge(npc, KRAKEN, attacker) else hit.damage = 0
            TENTACLE_WHIRLPOOL -> attacker?.let { emerge(npc, TENTACLE, it) }
            CAVE_KRAKEN_WHIRLPOOL -> attacker?.let { emerge(npc, CAVE_KRAKEN, it) }
            KRAKEN, CAVE_KRAKEN -> reduce(hit)
        }
    }

    fun canExplode(whirlpool: Npc): Boolean = whirlpool.visType.internalName == KRAKEN_WHIRLPOOL

    fun useExplosive(whirlpool: Npc, player: Player) {
        if (!canExplode(whirlpool)) return
        for (tentacle in nearby(whirlpool, TENTACLE_WHIRLPOOL)) {
            if (tentacle.visType.internalName == TENTACLE_WHIRLPOOL) emerge(tentacle, TENTACLE, player)
        }
        emerge(whirlpool, KRAKEN, player)
    }

    fun onKrakenDeath(kraken: Npc) {
        for (tentacle in nearby(kraken, TENTACLE_WHIRLPOOL) + nearby(kraken, TENTACLE)) {
            disturbed.remove(tentacle)
            if (tentacle.visType.internalName == TENTACLE) submerge(tentacle)
        }
        disturbed.removeIf { !it.isSlotAssigned }
    }

    fun submerge(npc: Npc) {
        npc.resetMode()
        npc.resetTransmog()
        npc.hitpoints = npc.baseHitpointsLvl
    }

    private fun emerge(npc: Npc, into: String, player: Player) {
        val type = ServerCacheManager.getNpc(into.asRSCM(RSCMType.NPC)) ?: return
        npc.transmog(type, Int.MAX_VALUE)
        npc.apRangeOverride = ATTACK_RANGE
        npc.apRequiresLineOfSight = false
        if (into != TENTACLE) npc.anim(ARISE_SEQ)
        if (into == TENTACLE) disturbed += npc
        deps.worldQueues.add(1) {
            if (npc.isSlotAssigned && npc.hitpoints > 0) npc.apPlayer2(player, aiPlayerInteractions)
        }
    }

    private fun tentaclesDisturbed(boss: Npc): Boolean =
        disturbed.count { it.isSlotAssigned && it.coords.chebyshevDistance(boss.coords) <= LAIR_RADIUS } >=
            KrakenCove.TENTACLE_WHIRLPOOLS.size

    private fun nearby(npc: Npc, baseType: String): List<Npc> {
        val id = baseType.asRSCM(RSCMType.NPC)
        return deps.npcRepo
            .findAll(ZoneKey.from(npc.coords), ZONE_RADIUS)
            .filter { it.id == id && it.coords.chebyshevDistance(npc.coords) <= LAIR_RADIUS }
            .toList()
    }

    private fun reduce(hit: HitBuilder) {
        when (hit.type) {
            HitType.Melee -> hit.damage = 0
            HitType.Ranged -> if (hit.damage > 0) hit.damage = maxOf(1, hit.damage / RANGED_DIVISOR)
            else -> Unit
        }
    }

    private fun attacker(hit: HitBuilder): Player? {
        if (!hit.isFromPlayer) return null
        val slot = hit.sourceSlot ?: return null
        return deps.playerList[slot]
    }

    fun lastAttacker(npc: Npc): Player? = deps.encounter(npc).lastTarget

    private companion object {
        const val ATTACK_RANGE = 10
        const val LAIR_RADIUS = 12
        const val ZONE_RADIUS = 2
        const val RANGED_DIVISOR = 7
        const val ARISE_SEQ = "seq.slayer_kraken_arise"
    }
}

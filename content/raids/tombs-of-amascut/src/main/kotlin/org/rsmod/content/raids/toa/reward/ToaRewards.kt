package org.rsmod.content.raids.toa.reward

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.raids.cox.reward.CoxRewardDelivery
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.party.ToaMode
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Singleton
class ToaRewards
@Inject
constructor(
    private val random: GameRandom,
    private val locRepo: LocRepository,
    private val delivery: CoxRewardDelivery,
) {
    fun recordCompletion(raid: ToaRaid) {
        for (member in raid.insiders) {
            when (raid.mode) {
                ToaMode.Entry -> member.toaEntryKc += 1
                ToaMode.Normal -> member.toaKc += 1
                ToaMode.Expert -> member.toaExpertKc += 1
            }
        }
    }

    fun deal(raid: ToaRaid) {
        val members = raid.insiders.toList().shuffled(java.util.Random(random.of(Int.MAX_VALUE).toLong()))
        val totalPoints = members.sumOf { raid.pointsOf(it) }
        var uniqueGiven = false
        for ((index, member) in members.withIndex()) {
            val points = raid.pointsOf(member)
            val permille = ToaLoot.uniquePermille(raid.raidLevel, points, totalPoints, members.size)
            val unique = !uniqueGiven && random.of(1000) < permille
            uniqueGiven = uniqueGiven || unique
            val loot = ToaLoot.roll(raid.raidLevel, points, unique, { delivery.owns(member, it) }) { random.of(it) }
            val rare = loot.any { it.obj in ToaLoot.UNIQUE_OBJS }
            val tile = raid.coords(ToaRoom.Vault, CHESTS[index % CHESTS.size].at)
            val angle = CHESTS[index % CHESTS.size].angle
            if (rare) {
                val sarcophagus = raid.coords(ToaRoom.Vault, SARCOPHAGUS)
                locRepo.add(sarcophagus, SARCOPHAGUS_RARE, Int.MAX_VALUE, LocAngle.East, LocShape.CentrepieceStraight)
                raid.chests[sarcophagus] = member
            } else {
                locRepo.add(tile, CHEST, Int.MAX_VALUE, angle, LocShape.CentrepieceStraight)
                raid.chests[tile] = member
            }
            raid.rewards[member] = loot
            member.mes("<col=ef1020>Your reward is waiting in the burial chamber.</col>")
        }
        for (winner in members) {
            val item = raid.rewards[winner]?.firstOrNull { it.obj in ToaLoot.UNIQUE_OBJS } ?: continue
            val line = "<col=ef20ff>${winner.displayName} found something special: ${delivery.displayName(item)}</col>"
            for (member in members) member.mes(line)
        }
    }

    fun claimLeftover(player: Player, raid: ToaRaid) {
        val loot = raid.rewards.remove(player) ?: return
        val bank = player.invMap.getOrPut("inv.bank")
        for (item in loot) delivery.give(player, item, bank)
        player.mes("Your unclaimed rewards have been sent to your bank.")
    }

    class Chest(val at: CoordGrid, val angle: LocAngle)

    companion object {
        const val CHEST = "loc.toa_vault_chest_mine_standard"
        const val SARCOPHAGUS_RARE = "loc.toa_vault_sarcophagus_closed_rare"
        val SARCOPHAGUS = CoordGrid(3679, 5140, 0)
        val CHESTS =
            listOf(
                Chest(CoordGrid(3686, 5145, 0), LocAngle.North),
                Chest(CoordGrid(3673, 5145, 0), LocAngle.South),
                Chest(CoordGrid(3686, 5142, 0), LocAngle.North),
                Chest(CoordGrid(3673, 5142, 0), LocAngle.South),
                Chest(CoordGrid(3686, 5148, 0), LocAngle.North),
                Chest(CoordGrid(3673, 5148, 0), LocAngle.South),
                Chest(CoordGrid(3686, 5139, 0), LocAngle.North),
                Chest(CoordGrid(3673, 5139, 0), LocAngle.South),
            )
    }
}

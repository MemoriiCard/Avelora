package org.rsmod.content.raids.tob.reward

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.raids.cox.reward.CoxRewardDelivery
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.content.raids.tob.raid.TobRaid
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Singleton
class TobRewards
@Inject
constructor(
    private val random: GameRandom,
    private val locRepo: LocRepository,
    private val delivery: CoxRewardDelivery,
) {
    fun recordCompletion(raid: TobRaid) {
        if (!raid.mode.awardsUniques) return
        for (member in raid.insiders) member.tobKc += 1
    }

    fun deal(raid: TobRaid) {
        val members = raid.insiders.toList()
        for ((index, member) in members.withIndex()) {
            val loot = TobLoot.roll(raid.mode) { random.of(it) }
            val rare = loot.any { entry -> TobLoot.UNIQUES.any { it.obj == entry.obj } }
            val tile = raid.coords(TobRoom.Treasure, CHESTS[index % CHESTS.size])
            val chest = if (rare) CHEST_RARE else CHEST
            locRepo.add(tile, chest, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
            raid.chests[tile] = member
            raid.rewards[member] = loot
            member.mes("<col=ef1020>Your Monumental chest is ready.</col>")
        }
        val shown =
            members.filter { m ->
                raid.rewards[m]?.any { r -> TobLoot.UNIQUES.any { it.obj == r.obj } } == true
            }
        for (winner in shown) {
            val item = raid.rewards.getValue(winner).first { r -> TobLoot.UNIQUES.any { it.obj == r.obj } }
            val line =
                "<col=ef20ff>${winner.displayName} found something special: ${delivery.displayName(item)}</col>"
            for (member in members) member.mes(line)
        }
    }

    fun claimLeftover(player: Player, raid: TobRaid) {
        val loot = raid.rewards.remove(player) ?: return
        val bank = player.invMap.getOrPut("inv.bank")
        for (item in loot) delivery.give(player, item, bank)
        player.mes("Your unclaimed chest rewards have been sent to your bank.")
    }

    companion object {
        const val CHEST = "loc.tob_treasureroom_chest_mine_standard"
        const val CHEST_RARE = "loc.tob_treasureroom_chest_mine_rare"
        val CHESTS =
            listOf(
                CoordGrid(3226, 4322, 0),
                CoordGrid(3242, 4322, 0),
                CoordGrid(3226, 4330, 0),
                CoordGrid(3227, 4319, 0),
                CoordGrid(3243, 4313, 0),
            )
    }
}

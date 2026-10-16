package org.rsmod.content.minigames.framework

import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.output.mes
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Player

class CrateDrop(val obj: String?, val min: Int, val max: Int, val weight: Int, val tokens: Boolean = false)

object HubCrates {
    const val CRATE = "obj.minigame_crate"
    const val DENOMINATOR = 1000

    val TABLE =
        listOf(
            CrateDrop(null, 40, 120, 470, tokens = true),
            CrateDrop("obj.4dosestamina", 2, 4, 90),
            CrateDrop("obj.4doseprayerrestore", 2, 4, 90),
            CrateDrop("obj.4dosepotionofsaradomin", 2, 4, 80),
            CrateDrop("obj.4dosestatrestore", 2, 4, 80),
            CrateDrop("obj.anglerfish", 8, 20, 130),
            CrateDrop("obj.looting_bag", 1, 1, 30),
            CrateDrop("obj.armadyl_halo", 1, 1, 6),
            CrateDrop("obj.bandos_halo", 1, 1, 6),
            CrateDrop("obj.seren_halo", 1, 1, 6),
            CrateDrop("obj.zaros_halo", 1, 1, 6),
            CrateDrop("obj.brassica_halo", 1, 1, 6),
        )

    class Reward(val obj: String?, val amount: Int)

    fun roll(nextInt: (Int) -> Int): Reward {
        var pick = nextInt(DENOMINATOR)
        for (drop in TABLE) {
            if (pick < drop.weight) return Reward(drop.obj, drop.min + nextInt(drop.max - drop.min + 1))
            pick -= drop.weight
        }
        val fallback = TABLE.first()
        return Reward(fallback.obj, fallback.min)
    }

    /** Crates earned by a finished game: at most one, chance scaled like every other payout. */
    fun earned(botShare: Double, won: Boolean, nextInt: (Int) -> Int): Int {
        val chance = (if (won) WIN_PERCENT else LOSS_PERCENT) * Payout.botScale(botShare)
        return if (nextInt(100) < chance.toInt()) 1 else 0
    }

    fun award(player: Player, objs: ObjRepository, random: GameRandom, botShare: Double, won: Boolean) {
        if (earned(botShare, won) { random.of(it) } == 0) return
        player.invAddOrDrop(objs, CRATE, 1)
        player.mes("You receive a Minigame Crate.")
    }

    const val WIN_PERCENT = 60
    const val LOSS_PERCENT = 30
}

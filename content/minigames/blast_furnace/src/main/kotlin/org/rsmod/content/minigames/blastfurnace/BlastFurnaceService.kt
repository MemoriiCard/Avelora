package org.rsmod.content.minigames.blastfurnace

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.baseSmithingLvl
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player

private val Player.key: Long
    get() = checkNotNull(uuid)

@Singleton
internal class BlastFurnaceService @Inject constructor(private val clock: MapClock) {
    private val paidUntil = HashMap<Long, Int>()
    private val disabledUntil = HashMap<Long, Int>()
    private val carryingCoke = HashSet<Long>()

    fun hasAccess(player: Player): Boolean =
        player.baseSmithingLvl >= BlastFurnaceRules.FREE_ENTRY_SMITHING ||
            (paidUntil[player.key] ?: 0) > clock.cycle

    fun pay(player: Player) {
        paidUntil[player.key] = clock.cycle + BlastFurnaceRules.FEE_TICKS
    }

    fun minutesLeft(player: Player): Int =
        (((paidUntil[player.key] ?: 0) - clock.cycle).coerceAtLeast(0) + 99) / 100

    fun isDisabled(player: Player): Boolean = (disabledUntil[player.key] ?: 0) > clock.cycle

    fun hasCoke(player: Player): Boolean = player.key in carryingCoke

    fun takeCoke(player: Player) {
        carryingCoke += player.key
    }

    fun spendCoke(player: Player): Boolean = carryingCoke.remove(player.key)

    fun heat(player: Player, amount: Int) {
        val next = player.furnaceTemperature + amount
        if (next > BlastFurnaceRules.OVERHEAT_AT) {
            player.furnaceTemperature = BlastFurnaceRules.OVERHEAT_RESET
            disabledUntil[player.key] = clock.cycle + BlastFurnaceRules.OVERHEAT_DISABLED_TICKS
            player.mes("The furnace overheats and the machinery grinds to a halt.")
            return
        }
        player.furnaceTemperature = next
    }

    fun startSmelting(player: Player) {
        player.softTimer(TIMER, BlastFurnaceRules.SMELT_INTERVAL)
    }

    fun tick(player: Player) {
        if (clock.cycle % BlastFurnaceRules.DECAY_EVERY == 0 && player.furnaceTemperature > 0) {
            player.furnaceTemperature -= 1
        }
        if (player.totalOres() == 0) {
            player.clearSoftTimer(TIMER)
            return
        }
        if (isDisabled(player)) return
        if (player.furnaceTemperature < BlastFurnaceRules.MIN_TEMPERATURE) return
        if (player.totalBars() >= BlastFurnaceRules.MAX_BARS) return
        val bar = nextBar(player)
        if (bar == null) {
            player.coalRequired = missingCoal(player)
            return
        }
        for (ore in bar.ores) player.setOre(ore, player.ore(ore) - 1)
        player.furnaceCoal -= bar.coal
        player.setBars(bar, player.bars(bar) + 1)
        player.coalRequired = 0
        if (player.dispenserState == BlastFurnaceRules.DISPENSER_EMPTY ||
            player.dispenserState == BlastFurnaceRules.DISPENSER_COOLED
        ) {
            player.dispenserState = BlastFurnaceRules.DISPENSER_HOT
        }
    }

    private fun nextBar(player: Player): Bar? =
        Bar.smeltOrder.firstOrNull { BlastFurnaceRules.canSmelt(it, player::ore, player.furnaceCoal) }

    private fun missingCoal(player: Player): Int =
        Bar.smeltOrder
            .filter { bar -> bar.ores.all { player.ore(it) > 0 } }
            .minOfOrNull { it.coal - player.furnaceCoal }
            ?.coerceAtLeast(0) ?: 0

    internal companion object {
        const val TIMER = "timer.blast_furnace"
    }
}

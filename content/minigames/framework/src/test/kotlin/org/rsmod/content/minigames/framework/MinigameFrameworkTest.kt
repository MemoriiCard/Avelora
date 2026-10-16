package org.rsmod.content.minigames.framework

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinigameFrameworkTest {
    private val policy = BotPolicy(MinigameConfig.load())

    @Test
    fun `shipped config matches the design thresholds`() {
        val config = MinigameConfig.load()
        assertEquals(GameSettings(10, 8), config.settings(Minigame.CastleWars))
        assertEquals(GameSettings(8, 6), config.settings(Minigame.LastManStanding))
        assertEquals(GameSettings(5, 5), config.settings(Minigame.BarbarianAssault))
    }

    @Test
    fun `bots fill to target and switch off at the threshold`() {
        assertEquals(8, policy.botsNeeded(Minigame.CastleWars, 2))
        assertEquals(3, policy.botsNeeded(Minigame.CastleWars, 7))
        assertEquals(0, policy.botsNeeded(Minigame.CastleWars, 8))
        assertEquals(0, policy.botsNeeded(Minigame.PestControl, 1))
    }

    @Test
    fun `bots go to the smaller team first and keep teams even`() {
        assertEquals(listOf(1, 5), policy.distribute(Minigame.CastleWars, listOf(4, 0)))
        assertEquals(listOf(0, 3), policy.distribute(Minigame.CastleWars, listOf(5, 2)))
        assertEquals(listOf(0, 0), policy.distribute(Minigame.CastleWars, listOf(4, 4)))
    }

    @Test
    fun `bot scaling follows the design examples`() {
        assertEquals(0.88, Payout.botScale(0.2), 1e-9)
        assertEquals(0.70, Payout.botScale(0.5), 1e-9)
        assertEquals(0.5, Payout.botScale(0.9), 1e-9)
        assertEquals(0.5, Payout.botScale(1.0), 1e-9)
        assertEquals(0.2, policy.botShare(Minigame.CastleWars, 4) - 0.4, 1e-9)
    }

    @Test
    fun `win bonus first win and loss share stack on the scaled base`() {
        assertEquals(150, Payout.reward(100, 0.0, won = true, firstWinToday = false))
        assertEquals(300, Payout.reward(100, 0.0, won = true, firstWinToday = true))
        assertEquals(90, Payout.reward(100, 0.0, won = false, firstWinToday = false))
        assertEquals(75, Payout.reward(100, 1.0, won = true, firstWinToday = false))
    }

    @Test
    fun `bonus of the day rotates through every game`() {
        val seen = (0L until Minigame.entries.size).map { BonusOfTheDay.gameFor(it) }.toSet()
        assertEquals(Minigame.entries.toSet(), seen)
        val today = BonusOfTheDay.gameFor(5)
        assertEquals(2, BonusOfTheDay.multiplier(today, 5))
        assertEquals(1, BonusOfTheDay.multiplier(Minigame.entries.first { it != today }, 5))
    }
}

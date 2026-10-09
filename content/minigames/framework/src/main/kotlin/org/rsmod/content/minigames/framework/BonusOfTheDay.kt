package org.rsmod.content.minigames.framework

object BonusOfTheDay {
    const val MULTIPLIER = 2

    private val rotation = Minigame.entries

    fun gameFor(epochDay: Long): Minigame = rotation[Math.floorMod(epochDay, rotation.size.toLong()).toInt()]

    fun multiplier(game: Minigame, epochDay: Long): Int = if (gameFor(epochDay) == game) MULTIPLIER else 1
}

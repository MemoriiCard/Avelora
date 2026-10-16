package org.rsmod.content.minigames.framework

import org.rsmod.map.CoordGrid

class BoardEntry(val label: String, val destination: CoordGrid, val game: Minigame? = null)

object MinigameBoard {
    val ENTRIES =
        listOf(
            BoardEntry("Castle Wars", CoordGrid(2440, 3089, 0), Minigame.CastleWars),
            BoardEntry("Soul Wars", CoordGrid(2208, 2856, 0), Minigame.SoulWars),
            BoardEntry("Last Man Standing", CoordGrid(3143, 3637, 0), Minigame.LastManStanding),
            BoardEntry("Barbarian Assault", CoordGrid(2534, 3573, 0), Minigame.BarbarianAssault),
            BoardEntry("Pest Control", CoordGrid(2657, 2640, 0), Minigame.PestControl),
            BoardEntry("Wintertodt", CoordGrid(1630, 3961, 0)),
            BoardEntry("Tempoross", CoordGrid(3140, 2846, 0)),
            BoardEntry("Guardians of the Rift", CoordGrid(3615, 9481, 0)),
        )

    fun label(entry: BoardEntry, epochDay: Long): String =
        if (entry.game != null && BonusOfTheDay.gameFor(epochDay) == entry.game) {
            "${entry.label} (double points today)"
        } else {
            entry.label
        }
}

package org.rsmod.content.minigames.framework

enum class Minigame(
    val key: String,
    val teams: Int,
    val defaultTarget: Int,
    val defaultBotFreeAt: Int,
    val usesBots: Boolean = true,
) {
    CastleWars("castle_wars", teams = 2, defaultTarget = 10, defaultBotFreeAt = 8),
    SoulWars("soul_wars", teams = 2, defaultTarget = 10, defaultBotFreeAt = 8),
    LastManStanding("last_man_standing", teams = 1, defaultTarget = 8, defaultBotFreeAt = 6),
    BarbarianAssault("barbarian_assault", teams = 1, defaultTarget = 5, defaultBotFreeAt = 5),
    PestControl("pest_control", teams = 1, defaultTarget = 0, defaultBotFreeAt = 0, usesBots = false),
    FightPits("fight_pits", teams = 1, defaultTarget = 0, defaultBotFreeAt = 0, usesBots = false),
}

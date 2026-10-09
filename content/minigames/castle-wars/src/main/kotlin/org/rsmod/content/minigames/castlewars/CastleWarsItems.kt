package org.rsmod.content.minigames.castlewars

internal object CastleWarsItems {
    const val TICKET = "obj.castlewars_ticket"

    fun cloak(team: CwTeam): String =
        when (team) {
            CwTeam.Saradomin -> "obj.castlewars_cloak_saradomin"
            CwTeam.Zamorak -> "obj.castlewars_cloak_zamorak"
        }

    fun banner(flagOf: CwTeam): String =
        when (flagOf) {
            CwTeam.Saradomin -> "obj.castlewars_saradomin_banner"
            CwTeam.Zamorak -> "obj.castlewars_zamorak_banner"
        }
}

package org.rsmod.content.minigames.soulwars

internal object SoulWarsItems {
    const val FRAGMENT = "obj.soul_fragment"

    fun cape(team: SwTeam): String =
        when (team) {
            SwTeam.Blue -> "obj.soul_wars_blue_cape"
            SwTeam.Red -> "obj.soul_wars_red_cape"
        }

    fun avatar(team: SwTeam): String =
        when (team) {
            SwTeam.Blue -> "npc.soul_wars_avatar_blue"
            SwTeam.Red -> "npc.soul_wars_avatar_red"
        }
}

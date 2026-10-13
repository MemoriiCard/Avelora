package org.rsmod.content.minigames.soulwars

internal object SoulWarsItems {
    const val FRAGMENT = "obj.soul_fragment"
    const val BONES = "obj.soul_wars_bones"
    const val BANDAGES = "obj.soul_wars_bandages"
    const val POWER_POTION = "obj.soul_wars_buff_potion_4"
    const val EXPLOSIVE_POTION = "obj.soul_wars_explosive_potion"

    fun cape(team: SwTeam): String =
        when (team) {
            SwTeam.Blue -> "obj.soul_wars_blue_cape"
            SwTeam.Red -> "obj.soul_wars_red_cape"
        }

    fun barricade(team: SwTeam): String =
        when (team) {
            SwTeam.Blue -> "obj.soul_wars_barricade_blue"
            SwTeam.Red -> "obj.soul_wars_barricade_red"
        }

    fun avatar(team: SwTeam): String =
        when (team) {
            SwTeam.Blue -> "npc.soul_wars_avatar_blue"
            SwTeam.Red -> "npc.soul_wars_avatar_red"
        }
}

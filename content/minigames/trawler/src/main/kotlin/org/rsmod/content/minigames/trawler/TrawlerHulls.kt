package org.rsmod.content.minigames.trawler

import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

private var Player.hull00 by intVarBit("varbit.trawler_hull_00")
private var Player.hull01 by intVarBit("varbit.trawler_hull_01")
private var Player.hull02 by intVarBit("varbit.trawler_hull_02")
private var Player.hull03 by intVarBit("varbit.trawler_hull_03")
private var Player.hull04 by intVarBit("varbit.trawler_hull_04")
private var Player.hull05 by intVarBit("varbit.trawler_hull_05")
private var Player.hull06 by intVarBit("varbit.trawler_hull_06")
private var Player.hull07 by intVarBit("varbit.trawler_hull_07")
private var Player.hull08 by intVarBit("varbit.trawler_hull_08")
private var Player.hull09 by intVarBit("varbit.trawler_hull_09")
private var Player.hull10 by intVarBit("varbit.trawler_hull_10")
private var Player.hull11 by intVarBit("varbit.trawler_hull_11")
private var Player.hull12 by intVarBit("varbit.trawler_hull_12")
private var Player.hull13 by intVarBit("varbit.trawler_hull_13")
private var Player.hull14 by intVarBit("varbit.trawler_hull_14")
private var Player.hull15 by intVarBit("varbit.trawler_hull_15")

internal fun Player.setHull(hull: Int, value: Int) {
    when (hull) {
        0 -> hull00 = value
        1 -> hull01 = value
        2 -> hull02 = value
        3 -> hull03 = value
        4 -> hull04 = value
        5 -> hull05 = value
        6 -> hull06 = value
        7 -> hull07 = value
        8 -> hull08 = value
        9 -> hull09 = value
        10 -> hull10 = value
        11 -> hull11 = value
        12 -> hull12 = value
        13 -> hull13 = value
        14 -> hull14 = value
        else -> hull15 = value
    }
}

internal object TrawlerHullMap {
    private const val FIRST_X = 1885
    private const val NORTH_Z = 4826
    private const val SOUTH_Z = 4823
    private const val PER_SIDE = 8

    fun indexAt(x: Int, z: Int): Int? {
        val offset = x - FIRST_X
        if (offset !in 0 until PER_SIDE) return null
        return when (z) {
            NORTH_Z -> offset
            SOUTH_Z -> PER_SIDE + offset
            else -> null
        }
    }
}

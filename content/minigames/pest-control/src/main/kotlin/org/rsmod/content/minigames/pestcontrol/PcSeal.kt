package org.rsmod.content.minigames.pestcontrol

internal object PcSeal {
    const val COST = 10
    const val FULL_CHARGES = 8
    val CHARGES = 1..FULL_CHARGES

    fun symbol(charges: Int): String = "obj.pest_seal_$charges"
}

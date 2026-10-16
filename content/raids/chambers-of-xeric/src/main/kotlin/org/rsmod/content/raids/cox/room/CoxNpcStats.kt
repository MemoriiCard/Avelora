package org.rsmod.content.raids.cox.room

import org.rsmod.content.raids.cox.party.CoxScaling
import org.rsmod.game.entity.Npc

/** A raid NPC's wiki infobox stats for a maxed solo party, before party scaling. */
data class CoxNpcStats(
    val hitpoints: Int,
    val attack: Int = 1,
    val strength: Int = 1,
    val defence: Int = 1,
    val ranged: Int = 1,
    val magic: Int = 1,
    val defensiveMagic: Boolean = false,
    val tekton: Boolean = false,
    val crystal: Boolean = false,
    val single: Boolean = false,
) {
    fun applyTo(npc: Npc, scaling: CoxScaling.Snapshot, hitpointsOverride: Int? = null) {
        fun off(base: Int) = if (single) scaling.singleStat(base) else scaling.offence(base)
        fun def(base: Int) =
            if (single) scaling.singleStat(base) else scaling.defence(base, tekton, crystal)

        val hp =
            hitpointsOverride
                ?: if (single) scaling.singleHitpoints(hitpoints)
                else scaling.hitpoints(hitpoints, crystal)
        npc.baseHitpointsLvl = hp
        npc.hitpoints = hp
        npc.baseAttackLvl = off(attack)
        npc.attackLvl = npc.baseAttackLvl
        npc.baseStrengthLvl = off(strength)
        npc.strengthLvl = npc.baseStrengthLvl
        npc.baseRangedLvl = off(ranged)
        npc.rangedLvl = npc.baseRangedLvl
        npc.baseMagicLvl = if (defensiveMagic) def(magic) else off(magic)
        npc.magicLvl = npc.baseMagicLvl
        npc.baseDefenceLvl = def(defence)
        npc.defenceLvl = npc.baseDefenceLvl
    }
}

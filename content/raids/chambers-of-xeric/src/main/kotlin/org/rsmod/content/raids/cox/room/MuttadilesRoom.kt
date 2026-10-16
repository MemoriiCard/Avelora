package org.rsmod.content.raids.cox.room

import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.npc.clearInteractionRoute
import org.rsmod.api.npc.heal
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.baseWoodcuttingLvl
import org.rsmod.api.route.walkTo
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

/**
 * The small muttadile fights while the big one lurks underwater, spitting magic at anyone in
 * sight. Either one wounded below half health runs to the meat tree to feed, up to three times,
 * unless the party has chopped the tree down. The big one surfaces when the small one dies.
 */
class MuttadilesRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    CoxRoomController(raid, room, services) {
    private lateinit var small: Npc
    private lateinit var big: Npc
    private var tree: Npc? = null
    var treeHitpoints = 0
        private set

    private var submerged = true
    private val feeding = mutableMapOf<Npc, Int>()
    private val meals = mutableMapOf<Npc, Int>()

    override fun spawn() {
        val (poolX, poolZ) = pick(18 to 19, 18 to 17, 11 to 18)
        big = spawnNpc(BIG, poolX, poolZ, BIG_STATS)
        small = spawnNpc(SMALL, poolX, poolZ - 5, SMALL_STATS, ranged = true)
        transmog(big, SUBMERGED)
        big.hideAllOps()
        big.ignoreCombatInteractions = true

        val (treeX, treeZ) = pick(8 to 7, 8 to 7, 23 to 8)
        val treeTile = findLocs(TREE_ROOTS).firstOrNull()?.coords ?: standTile(local(treeX, treeZ), TREE_SIZE)
        val woodcutting = raid.party.members.sumOf { it.baseWoodcuttingLvl }
        treeHitpoints = maxOf(MIN_TREE_HITPOINTS, woodcutting * TREE_HP_PER_LEVEL)
        tree =
            spawnAt(TREE, treeTile, stats = null, required = false).also {
                it.baseHitpointsLvl = treeHitpoints
                it.hitpoints = treeHitpoints
                it.movementLocked = true
            }
    }

    override fun onEngage(first: Player) {
        services.boss.encounter(big).invulnerable = true
    }

    override fun onTick() {
        if (submerged && services.cycle % SUBMERGED_ATTACK_RATE == 0) submergedVolley()
        for (mutt in listOf(small, big)) {
            if (!mutt.isSlotAssigned || mutt.hitpoints <= 0) continue
            if (mutt === big && submerged) continue
            tickFeeding(mutt)
        }
    }

    override fun onNpcRemoved(npc: Npc) {
        if (npc === tree) {
            tree = null
            return
        }
        feeding.remove(npc)
        if (npc === small && submerged) surface()
    }

    /** Applies a chop to the tree; returns `true` once it has fallen. */
    fun chop(damage: Int): Boolean {
        val current = tree ?: return true
        treeHitpoints = (treeHitpoints - damage).coerceAtLeast(0)
        current.hitpoints = treeHitpoints
        if (treeHitpoints > 0) return false
        for (mutt in feeding.keys.toList()) stopFeeding(mutt)
        services.npcRepo.del(current, Int.MAX_VALUE)
        for (member in playersInRoom()) {
            member.mes("The meat tree is cut down. The muttadiles can no longer feed.")
        }
        return true
    }

    private fun submergedVolley() {
        for (player in playersInRoom()) {
            if (services.random.of(1, SUBMERGED_CHANCE) != 1) continue
            services.strike(big, player, HitType.Magic, delay = 2)
        }
    }

    private fun surface() {
        submerged = false
        big.resetTransmog()
        big.showAllOps()
        big.anim("seq.dohgadyle_emerge")
        big.ignoreCombatInteractions = false
        services.boss.encounter(big).invulnerable = false
        nearestPlayer(big)?.let { engage(big, it) }
    }

    private fun tickFeeding(mutt: Npc) {
        val started = feeding[mutt]
        if (started != null) {
            if (services.cycle - started > FEED_TIMEOUT) stopFeeding(mutt)
            return
        }
        val wounded = mutt.hitpoints * 2 <= mutt.baseHitpointsLvl
        val treeTile = tree?.coords ?: return
        if (!wounded || treeHitpoints <= 0 || (meals[mutt] ?: 0) >= MAX_MEALS) return
        feeding[mutt] = services.cycle
        mutt.clearInteractionRoute()
        mutt.ignoreCombatInteractions = true
        mutt.walkTo(services.routes, treeTile) { eat(mutt) }
    }

    private fun eat(mutt: Npc) {
        if (feeding.remove(mutt) == null || treeHitpoints <= 0) {
            resume(mutt)
            return
        }
        meals[mutt] = (meals[mutt] ?: 0) + 1
        mutt.anim("seq.dohgadyle_bite")
        val percent = services.random.of(MEAL_MIN_PERCENT, MEAL_MAX_PERCENT)
        mutt.heal(mutt.baseHitpointsLvl * percent / 100, showHitsplat = true)
        resume(mutt)
    }

    private fun stopFeeding(mutt: Npc) {
        feeding.remove(mutt)
        mutt.abortRoute()
        resume(mutt)
    }

    private fun resume(mutt: Npc) {
        mutt.ignoreCombatInteractions = false
        nearestPlayer(mutt)?.let { engage(mutt, it) }
    }

    companion object {
        const val SMALL = "npc.raids_dogodile_junior"
        const val BIG = "npc.raids_dogodile"
        const val SUBMERGED = "npc.raids_dogodile_submerged"
        const val TREE = "npc.raids_dogodile_meat_tree"
        private const val TREE_ROOTS = "loc.raids_meat_tree_full"

        val SMALL_STATS =
            CoxNpcStats(hitpoints = 250, attack = 150, strength = 150, defence = 138, ranged = 150)
        val BIG_STATS =
            CoxNpcStats(
                hitpoints = 250,
                attack = 250,
                strength = 250,
                defence = 220,
                ranged = 250,
                magic = 250,
            )

        private const val TREE_SIZE = 2
        private const val MIN_TREE_HITPOINTS = 100
        private const val TREE_HP_PER_LEVEL = 5
        private const val SUBMERGED_ATTACK_RATE = 4
        private const val SUBMERGED_CHANCE = 3
        private const val FEED_TIMEOUT = 25
        private const val MAX_MEALS = 3
        private const val MEAL_MIN_PERCENT = 40
        private const val MEAL_MAX_PERCENT = 50
    }
}

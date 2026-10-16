package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hunterLvl
import org.rsmod.api.player.stat.statRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FixedTrapScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val playerRepo: PlayerRepository,
    private val random: GameRandom,
) : PluginScript() {
    private val traps = FixedTraps()

    override fun ScriptContext.startup() {
        for (site in FixedSites.all) {
            onOpLoc1(site.idleLoc) { setTrap(site, it.loc) }
            onOpLoc1(site.setLoc) { dismantle(it.loc) }
        }
        for (knife in KNIVES) {
            onOpLocU(FixedSites.deadfall.idleLoc, knife) { setTrap(FixedSites.deadfall, it.loc) }
        }
        val fullLocs =
            FixedSites.deadfallPrey.map { it.fullLoc } + FixedSites.nets.map { it.second.fullLoc }
        for (full in fullLocs) {
            onOpLoc1(full) { check(it.loc) }
        }
        onPlayerSoftTimer(TIMER) { tickTraps(player) }
        onPlayerLogout { all(player).forEach { revert(it, notify = false) } }
    }

    private fun all(player: Player): List<FixedTrap> =
        FixedSites.all.flatMap { traps.ownedBy(player, it) }

    private fun ProtectedAccess.supplies(site: FixedSite): Supplies? {
        if (site === FixedSites.deadfall) {
            if (KNIVES.none { inv.contains(it) }) {
                mes("You need a knife to set up a deadfall.")
                return null
            }
            val log = LOGS.firstOrNull { inv.contains(it) }
            if (log == null) {
                mes("You need some logs to set up a deadfall.")
                return null
            }
            return Supplies(consumed = listOf(log), returned = emptyList(), preserveChance = LOG_KEEP_PERCENT)
        }
        if (inv.contains(NET_TRAP)) {
            return Supplies(consumed = listOf(NET_TRAP), returned = listOf(NET_TRAP))
        }
        if (inv.contains(NET) && inv.contains(ROPE)) {
            return Supplies(consumed = listOf(NET, ROPE), returned = listOf(NET, ROPE))
        }
        mes("You need a net trap, or a small fishing net and a rope, to set this up.")
        return null
    }

    private suspend fun ProtectedAccess.setTrap(site: FixedSite, loc: BoundLocInfo) {
        val tile = loc.coords
        val required = requiredLevel(site, loc)
        if (player.hunterLvl < required) {
            mes("You need a Hunter level of $required to set up a ${site.noun} here.")
            return
        }
        val max = site.maxTraps(player.hunterLvl)
        if (traps.ownedBy(player, site).size >= max) {
            mes("You don't have a high enough Hunter level to set up more than $max ${site.noun}s.")
            return
        }
        if (traps[tile] != null) {
            mes("There is already a trap here.")
            return
        }
        val supplies = supplies(site) ?: return
        mes("You begin setting up the ${site.noun}.")
        anim("seq.human_laytrap")
        delay(LAY_TICKS)
        if (traps[tile] != null || supplies.consumed.any { !inv.contains(it) }) {
            return
        }
        val keep = supplies.preserveChance > 0 && random.of(100) < supplies.preserveChance
        if (!keep) {
            for (obj in supplies.consumed) {
                invDel(inv, obj)
            }
        }
        val replaced = locRepo.del(loc, Int.MAX_VALUE)
        if (!replaced) {
            return
        }
        val set = locRepo.add(tile, site.setLoc, Int.MAX_VALUE, loc.angle, loc.shape)
        traps.add(FixedTrap(player, site, tile, set, FixedState.Set, mapClock + TRAP_LIFETIME, supplies.returned))
        player.softTimer(TIMER, CHECK_INTERVAL)
    }

    private fun requiredLevel(site: FixedSite, loc: BoundLocInfo): Int =
        if (site === FixedSites.deadfall) {
            FixedSites.deadfallPrey.minOf { it.level }
        } else {
            FixedSites.nets.first { it.first === site }.second.level
        }

    private suspend fun ProtectedAccess.dismantle(loc: BoundLocInfo) {
        val trap = ownTrapAt(loc) ?: return
        anim("seq.human_pickupfloor")
        delay(1)
        if (traps[loc.coords] !== trap) {
            return
        }
        restoreIdle(trap)
        for (obj in trap.returned) {
            invAddOrDrop(objRepo, obj)
        }
        mes("You dismantle the ${trap.site.noun}.")
    }

    private fun ProtectedAccess.ownTrapAt(loc: BoundLocInfo): FixedTrap? {
        val trap = traps[loc.coords]
        if (trap == null || trap.loc.id != loc.entity.id) {
            return null
        }
        if (trap.owner !== player) {
            mes("This isn't your trap.")
            return null
        }
        return trap
    }

    private suspend fun ProtectedAccess.check(loc: BoundLocInfo) {
        val trap = ownTrapAt(loc) ?: return
        val prey = trap.prey ?: return
        val loot = prey.loot.map { it.obj to random.of(it.min..it.max) } + trap.returned.map { it to 1 }
        anim("seq.human_pickupfloor")
        delay(1)
        if (traps[loc.coords] !== trap) {
            return
        }
        val result =
            player.invTransaction(inv) {
                val into = select(inv)
                for ((obj, count) in loot) {
                    insert {
                        this.into = into
                        this.obj = obj.asRSCM(RSCMType.OBJ)
                        this.strictCount = count
                    }
                }
            }
        if (result.failure) {
            mes("You don't have enough inventory space to do that.")
            return
        }
        restoreIdle(trap)
        statAdvance(STAT, prey.xp)
        mes("You've caught a ${prey.name}.")
    }

    private fun tickTraps(player: Player) {
        val owned = all(player)
        if (owned.isEmpty()) {
            player.clearSoftTimer(TIMER)
            return
        }
        val clock = player.currentMapClock
        for (trap in owned) {
            val tooFar =
                trap.coords.level != player.coords.level ||
                    trap.coords.chebyshevDistance(player.coords) > MAX_DISTANCE
            if (clock >= trap.expiresAt || tooFar) {
                revert(trap, notify = true)
                continue
            }
            when (trap.state) {
                FixedState.Set -> attract(player, trap, clock)
                FixedState.Catching -> settle(trap, FixedState.Caught, checkNotNull(trap.prey).fullLoc, clock)
                FixedState.Failing -> revert(trap, notify = false)
                FixedState.Caught -> {}
            }
        }
    }

    private fun attract(player: Player, trap: FixedTrap, clock: Int) {
        if (clock < trap.expiresAt - TRAP_LIFETIME + SET_DELAY || playerRepo.findAll(trap.coords).any()) {
            return
        }
        val isDeadfall = trap.site === FixedSites.deadfall
        val (npcHit, prey) = if (isDeadfall) nearbyPrey(trap, player.hunterLvl) ?: return else null to netCatch(trap)
        if (random.of(if (isDeadfall) ATTRACT_CHANCE else NET_ATTRACT_CHANCE) != 0) {
            return
        }
        if (player.hunterLvl < prey.level) {
            return
        }
        val caught = player.statRandom(random, STAT, prey.catchLow, prey.catchHigh, invisibleBoost = 0)
        if (caught) {
            npcHit?.let { npcRepo.hide(it, PREY_RESPAWN_TICKS) }
            trap.prey = prey
            val mirror = npcHit != null && npcHit.coords.x < trap.coords.x
            replaceLoc(trap, if (mirror) "${prey.trappingLoc}_m" else prey.trappingLoc)
            trap.state = FixedState.Catching
        } else {
            replaceLoc(trap, trap.site.failingLoc)
            trap.state = FixedState.Failing
        }
        trap.expiresAt = clock + TRAP_LIFETIME
    }

    private fun netCatch(trap: FixedTrap): FixedCatch =
        FixedSites.nets.first { it.first === trap.site }.second

    private fun nearbyPrey(trap: FixedTrap, hunterLevel: Int): Pair<org.rsmod.game.entity.Npc, FixedCatch>? =
        npcRepo
            .findAll(ZoneKey.from(trap.coords), zoneRadius = 1)
            .filter { it.isVisible && it.coords.level == trap.coords.level }
            .filter { it.coords.chebyshevDistance(trap.coords) <= LURE_RADIUS }
            .mapNotNull { npc -> preyByNpc[npc.type.id]?.let { npc to it } }
            .filter { (_, prey) -> prey.level <= hunterLevel }
            .minByOrNull { (npc, _) -> npc.coords.chebyshevDistance(trap.coords) }

    private fun settle(trap: FixedTrap, state: FixedState, loc: String, clock: Int) {
        replaceLoc(trap, loc)
        trap.state = state
        trap.expiresAt = clock + TRAP_LIFETIME
    }

    private fun replaceLoc(trap: FixedTrap, internal: String) {
        val old = trap.loc
        locRepo.del(old, Int.MAX_VALUE)
        trap.loc = locRepo.add(old.coords, internal, Int.MAX_VALUE, old.angle, old.shape)
    }

    private fun restoreIdle(trap: FixedTrap) {
        replaceLoc(trap, trap.site.idleLoc)
        traps.remove(trap)
    }

    private fun revert(trap: FixedTrap, notify: Boolean) {
        restoreIdle(trap)
        val player = trap.owner
        for (obj in trap.returned) {
            objRepo.add(obj, trap.coords, COLLAPSED_ITEM_TICKS, receiver = player)
        }
        if (notify) {
            player.mes("Your ${trap.site.noun} has collapsed.")
        }
    }

    private class Supplies(
        val consumed: List<String>,
        val returned: List<String>,
        val preserveChance: Int = 0,
    )

    private companion object {
        const val STAT = "stat.hunter"
        const val TIMER = "timer.hunter_fixed_traps"
        const val CHECK_INTERVAL = 3
        const val LAY_TICKS = 3
        const val SET_DELAY = 8
        const val LURE_RADIUS = 3
        const val ATTRACT_CHANCE = 2
        const val NET_ATTRACT_CHANCE = 6
        const val TRAP_LIFETIME = 200
        const val COLLAPSED_ITEM_TICKS = 200
        const val PREY_RESPAWN_TICKS = 25
        const val MAX_DISTANCE = 20
        const val LOG_KEEP_PERCENT = 75
        const val NET_TRAP = "obj.net_trap"
        const val NET = "obj.net"
        const val ROPE = "obj.rope"

        val KNIVES = listOf("obj.knife", "obj.fletching_knife")
        val LOGS =
            listOf(
                "obj.logs",
                "obj.oak_logs",
                "obj.willow_logs",
                "obj.teak_logs",
                "obj.maple_logs",
                "obj.mahogany_logs",
                "obj.yew_logs",
                "obj.magic_logs",
            )

        val preyByNpc: Map<Int, FixedCatch> by lazy {
            FixedSites.deadfallPrey.associateBy { checkNotNull(it.npc).asRSCM(RSCMType.NPC) }
        }
    }
}

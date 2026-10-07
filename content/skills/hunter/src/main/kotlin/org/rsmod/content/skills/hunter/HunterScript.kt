package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.map.MapSingletons.collision
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
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.collision.firstStepDestination
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HunterScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val playerRepo: PlayerRepository,
    private val random: GameRandom,
) : PluginScript() {
    private val traps = HunterTraps()
    private val walkDirections = listOf(Direction.West, Direction.East, Direction.South, Direction.North)

    override fun ScriptContext.startup() {
        for (kind in TrapKind.entries) {
            onOpHeld1(kind.obj) { layTrap(kind) }
            onOpLoc1(kind.setLoc) { pickUp(it.loc) }
            onOpLoc1(kind.brokenLoc) { pickUp(it.loc) }
        }
        for (fullLoc in HunterCreatures.all.map { it.fullLoc }.distinct()) {
            onOpLoc1(fullLoc) { check(it.loc) }
        }
        onPlayerSoftTimer(TIMER) { tickTraps(player) }
        onPlayerLogout { traps.ownedBy(player).forEach { collapse(it, notify = false) } }
    }

    private suspend fun ProtectedAccess.layTrap(kind: TrapKind) {
        if (player.hunterLvl < kind.level) {
            mes("You need a Hunter level of ${kind.level} to set up a ${kind.noun}.")
            return
        }
        val max = HunterCreatures.maxTraps(player.hunterLvl)
        if (traps.ownedBy(player).size >= max) {
            mes("You don't have a high enough Hunter level to set up more than $max traps.")
            return
        }
        val tile = coords
        if (!canLayAt(tile)) {
            mes("You can't lay a trap here.")
            return
        }
        mes("You begin setting up the trap.")
        anim("seq.human_laytrap")
        delay(LAY_TICKS)
        if (coords != tile || !canLayAt(tile) || invDel(inv, kind.obj).failure) {
            return
        }
        val loc = locRepo.add(tile, kind.setLoc, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
        traps.add(Trap(player, kind, loc, TrapState.Set, mapClock + TRAP_LIFETIME))
        player.softTimer(TIMER, CHECK_INTERVAL)
        collision.firstStepDestination(tile, walkDirections)?.let { walk(it) }
    }

    private fun canLayAt(tile: CoordGrid): Boolean =
        traps[tile] == null &&
            locRepo.findExact(tile, LocShape.CentrepieceStraight) == null &&
            locRepo.findExact(tile, LocShape.CentrepieceDiagonal) == null

    private fun ProtectedAccess.ownTrapAt(loc: BoundLocInfo): Trap? {
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

    private suspend fun ProtectedAccess.pickUp(loc: BoundLocInfo) {
        val trap = ownTrapAt(loc) ?: return
        if (inv.isFull() && !inv.contains(trap.kind.obj)) {
            mes("You don't have enough inventory space to do that.")
            return
        }
        anim("seq.human_pickupfloor")
        delay(1)
        if (traps[loc.coords] !== trap) {
            return
        }
        remove(trap)
        invAdd(inv, trap.kind.obj)
        mes("You dismantle the trap.")
    }

    private suspend fun ProtectedAccess.check(loc: BoundLocInfo) {
        val trap = ownTrapAt(loc) ?: return
        val creature = trap.creature ?: return
        val loot = creature.loot.map { it.obj to random.of(it.min..it.max) } + (trap.kind.obj to 1)
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
        remove(trap)
        statAdvance(STAT, creature.xp)
        mes("You've caught ${creature.displayName()}.")
    }

    private fun tickTraps(player: Player) {
        val owned = traps.ownedBy(player)
        if (owned.isEmpty()) {
            player.clearSoftTimer(TIMER)
            return
        }
        val clock = player.currentMapClock
        for (trap in owned) {
            val coords = trap.loc.coords
            val tooFar = coords.level != player.coords.level || coords.chebyshevDistance(player.coords) > MAX_DISTANCE
            if (clock >= trap.expiresAt || tooFar) {
                collapse(trap, notify = true)
                continue
            }
            when (trap.state) {
                TrapState.Set -> attract(player, trap, clock)
                TrapState.Catching -> settle(trap, TrapState.Caught, checkNotNull(trap.creature).fullLoc, clock)
                TrapState.Failing -> settle(trap, TrapState.Broken, trap.kind.brokenLoc, clock)
                TrapState.Caught,
                TrapState.Broken -> {}
            }
        }
    }

    private fun attract(player: Player, trap: Trap, clock: Int) {
        val coords = trap.loc.coords
        if (playerRepo.findAll(coords).any()) {
            return
        }
        val prey = nearbyPrey(trap, player.hunterLvl) ?: return
        if (random.of(ATTRACT_CHANCE) != 0) {
            return
        }
        val (npc, creature) = prey
        val caught = player.statRandom(random, STAT, creature.catchLow, creature.catchHigh, invisibleBoost = 0)
        if (caught) {
            npcRepo.hide(npc, PREY_RESPAWN_TICKS)
            trap.creature = creature
            replaceLoc(trap, creature.trappingLoc.format(sideOf(npc.coords, coords)))
            trap.state = TrapState.Catching
        } else {
            replaceLoc(trap, trap.kind.failingLoc)
            trap.state = TrapState.Failing
        }
        trap.expiresAt = clock + TRAP_LIFETIME
    }

    private fun nearbyPrey(trap: Trap, hunterLevel: Int): Pair<Npc, HunterCreature>? {
        val coords = trap.loc.coords
        return npcRepo
            .findAll(ZoneKey.from(coords), zoneRadius = 1)
            .filter { it.isVisible && it.coords.level == coords.level }
            .filter { it.coords.chebyshevDistance(coords) <= LURE_RADIUS }
            .mapNotNull { npc -> creaturesByNpc[npc.type.id]?.let { npc to it } }
            .filter { (_, creature) -> creature.trap == trap.kind && creature.level <= hunterLevel }
            .minByOrNull { (npc, _) -> npc.coords.chebyshevDistance(coords) }
    }

    private fun settle(trap: Trap, state: TrapState, loc: String, clock: Int) {
        replaceLoc(trap, loc)
        trap.state = state
        trap.expiresAt = clock + TRAP_LIFETIME
    }

    private fun replaceLoc(trap: Trap, internal: String) {
        val old = trap.loc
        locRepo.del(old, Int.MAX_VALUE)
        trap.loc = locRepo.add(old.coords, internal, Int.MAX_VALUE, old.angle, old.shape)
    }

    private fun remove(trap: Trap) {
        locRepo.del(trap.loc, Int.MAX_VALUE)
        traps.remove(trap)
    }

    private fun collapse(trap: Trap, notify: Boolean) {
        remove(trap)
        objRepo.add(trap.kind.obj, trap.loc.coords, COLLAPSED_ITEM_TICKS, receiver = trap.owner)
        if (notify) {
            trap.owner.mes("Your ${trap.kind.noun} has collapsed.")
        }
    }

    private fun sideOf(from: CoordGrid, trap: CoordGrid): String {
        val dx = from.x - trap.x
        val dz = from.z - trap.z
        return when {
            kotlin.math.abs(dx) >= kotlin.math.abs(dz) -> if (dx > 0) "e" else "w"
            dz > 0 -> "n"
            else -> "s"
        }
    }

    private fun HunterCreature.displayName(): String {
        val name = ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC))?.name ?: "something"
        val article = if (name.first().lowercaseChar() in "aeiou") "an" else "a"
        return "$article ${name.lowercase()}"
    }

    private companion object {
        const val STAT = "stat.hunter"
        const val TIMER = "timer.hunter_traps"
        const val CHECK_INTERVAL = 3
        const val LAY_TICKS = 3
        const val LURE_RADIUS = 2
        const val ATTRACT_CHANCE = 2
        const val TRAP_LIFETIME = 200
        const val COLLAPSED_ITEM_TICKS = 200
        const val PREY_RESPAWN_TICKS = 25
        const val MAX_DISTANCE = 20

        val creaturesByNpc: Map<Int, HunterCreature> by lazy {
            HunterCreatures.all.associateBy { it.npc.asRSCM(RSCMType.NPC) }
        }
    }
}

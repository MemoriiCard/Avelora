package org.rsmod.content.minigames.gotr

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.stat.runecraftingLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

private var Player.pointsVar by intVarp(GotrService.POINTS)

@Singleton
class GotrService
@Inject
constructor(
    private val players: PlayerList,
    private val clock: MapClock,
    private val npcs: NpcRepository,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
    private val events: EventBus,
) {
    val round = GotrRound { random.of(it) }

    private val inside = HashSet<Int>()
    private val guardianNpcs = ArrayList<Pair<Int, Npc>>()
    private var countdown = -1
    private var lastTick = -1

    val waiting: Boolean
        get() = countdown >= 0

    fun isInside(player: Player): Boolean = player.slotId in inside

    fun enter(player: Player) {
        if (player.runecraftingLvl < MIN_RUNECRAFT) {
            player.mes("You need a Runecraft level of $MIN_RUNECRAFT to take part in the Guardians of the Rift.")
            return
        }
        if (isInside(player)) return
        inside += player.slotId
        player.ifOpenOverlay(OVERLAY, events)
        player.softTimer(TIMER, 1)
        when {
            round.active -> teleport(player, GotrMap.GAME)
            countdown < 0 -> {
                countdown = WAIT_TICKS
                player.mes("The Great Guardian will stir in a minute. Stay in the lobby until the barrier lowers.")
            }
            else -> player.mes("You are registered. The barrier lowers when the Great Guardian stirs.")
        }
    }

    fun leave(player: Player, toLobby: Boolean = true) {
        if (!inside.remove(player.slotId)) return
        round.forfeit(player.slotId)
        player.clearSoftTimer(TIMER)
        player.ifCloseOverlay(OVERLAY, events)
        if (toLobby) teleport(player, GotrMap.LOBBY_BARRIER_SIDE)
        if (inside.isEmpty()) shutdown()
    }

    fun passBarrier(player: Player) {
        if (isInside(player) && player.coords.z > GotrMap.BARRIER_Z) {
            leave(player)
        } else {
            enter(player)
        }
    }

    fun tick() {
        val cycle = clock.cycle
        if (cycle == lastTick) return
        lastTick = cycle
        if (countdown > 0 && --countdown == 0) begin(cycle)
        if (round.active) apply(round.tick(cycle))
        if (round.active) refreshOverlays(cycle)
        if (round.phase == GotrPhase.Won) finish(won = true)
        if (round.phase == GotrPhase.Lost) finish(won = false)
    }

    fun assemble(player: Player, kind: GotrKind): Boolean {
        val energy = round.assemble(player.slotId, kind, clock.cycle)
        if (energy < 0) return false
        spawnGuardian(round.rune(kind), GotrMap.GUARDIAN_SPAWN.getValue(kind))
        player.mes("You assemble a ${round.rune(kind).symbol} guardian (+$energy ${kind.name.lowercase()} energy).")
        return true
    }

    fun placeCell(player: Player, tile: Int, tier: GotrTier): Boolean =
        round.placeCell(player.slotId, tile, tier, clock.cycle)

    fun deposit(player: Player, stones: Int, polyStones: Int) {
        round.deposit(stones, polyStones)
        player.mes("The Great Guardian's power surges (${(round.power * 100 / round.maxPower).toInt()}%).")
    }

    fun searchesOf(player: Player): Pair<Int, Int> = player.pointsVar.let { (it and MASK) to (it ushr SHIFT and MASK) }

    fun spendSearch(player: Player) {
        val (ele, cat) = searchesOf(player)
        player.pointsVar = ((cat - 1) shl SHIFT) or (ele - 1)
    }

    private fun begin(cycle: Int) {
        countdown = -1
        round.start(inside.size, cycle)
        for (id in inside) players[id]?.let { teleport(it, GotrMap.GAME) }
        broadcast("The Great Guardian awakens! Keep it charged with guardian stones.")
    }

    private fun apply(list: List<GotrEvent>) {
        for (event in list) {
            when (event) {
                is GotrEvent.Rotated -> broadcast("The portals shift: ${event.elemental.name} and ${event.catalytic.name} are open.")
                is GotrEvent.GuardiansExpired -> trimGuardians(event.count)
                GotrEvent.Won,
                GotrEvent.Lost -> Unit
            }
        }
    }

    private fun finish(won: Boolean) {
        val everyone = inside.toList()
        for (id in everyone) {
            val player = players[id] ?: continue
            if (!won) {
                player.mes("The Great Guardian's power fades and the rift remains open.")
                continue
            }
            val reward = round.reward(id)
            if (!reward.xpBonus) {
                player.mes("The rift closes, but you needed ${GotrRound.MIN_REWARD_ENERGY} energy for a reward.")
                continue
            }
            player.statAdvance("stat.runecrafting", player.runecraftingLvl * XP_PER_LEVEL.toDouble())
            val (ele, cat) = searchesOf(player)
            val newEle = (ele + reward.elementalPoints).coerceAtMost(GotrLoot.MAX_POINTS)
            val newCat = (cat + reward.catalyticPoints).coerceAtMost(GotrLoot.MAX_POINTS)
            player.pointsVar = (newCat shl SHIFT) or newEle
            player.mes("The rift closes! You earned ${reward.elementalPoints} elemental and ${reward.catalyticPoints} catalytic points.")
        }
        for (id in everyone) players[id]?.let { leave(it) }
        inside.clear()
        shutdown()
    }

    private fun shutdown() {
        round.reset()
        countdown = -1
        for ((_, npc) in guardianNpcs) if (npc.isSlotAssigned) npcs.del(npc, Int.MAX_VALUE)
        guardianNpcs.clear()
    }

    private fun spawnGuardian(rune: GotrRune, coords: CoordGrid) {
        val symbol = "npc.gotr_guardian_${rune.symbol}"
        val id = runCatching { symbol.asRSCM(RSCMType.NPC) }.getOrNull() ?: return
        val type = ServerCacheManager.getNpc(id) ?: return
        val npc = Npc(type, coords)
        npcs.add(npc, Int.MAX_VALUE)
        guardianNpcs += clock.cycle + GotrRound.GUARDIAN_LIFE to npc
    }

    private fun trimGuardians(count: Int) {
        repeat(count) {
            val entry = guardianNpcs.minByOrNull { it.first } ?: return
            guardianNpcs.remove(entry)
            if (entry.second.isSlotAssigned) npcs.del(entry.second, Int.MAX_VALUE)
        }
    }

    private fun refreshOverlays(cycle: Int) {
        val script = "clientscript.[clientscript,gotr_hud_update]".asRSCM(RSCMType.CLIENTSCRIPT)
        val portalTicks = (round.nextRotation - cycle).coerceAtLeast(0)
        val elementalIndex = GotrRune.elemental.indexOf(round.elementalRune)
        val catalyticIndex = CATALYTIC_ORDER.indexOf(round.catalyticRune)
        forEachInside { player ->
            player.runClientScript(
                script,
                round.energyOf(player.slotId, GotrKind.Elemental),
                round.energyOf(player.slotId, GotrKind.Catalytic),
                round.power.toInt(),
                round.maxPower,
                0,
                elementalIndex,
                catalyticIndex,
                round.guardianCount,
                GotrRound.GUARDIAN_LIMIT,
                portalTicks,
                -1,
            )
        }
    }

    private fun broadcast(message: String) = forEachInside { it.mes(message) }

    private fun forEachInside(action: (Player) -> Unit) {
        for (id in inside.toList()) {
            val player = players[id]
            if (player == null) {
                inside.remove(id)
                round.forfeit(id)
                continue
            }
            action(player)
        }
    }

    private fun teleport(player: Player, dest: CoordGrid) {
        PathingEntityCommon.telejump(player, collision, dest)
    }

    companion object {
        const val TIMER = "timer.gotr"
        const val OVERLAY = "interface.gotr_hud"
        const val POINTS = "varp.gotr_rewards_perm"
        const val MIN_RUNECRAFT = 27
        const val WAIT_TICKS = 100
        const val XP_PER_LEVEL = 45
        const val SHIFT = 16
        const val MASK = 0xFFFF

        val CATALYTIC_ORDER =
            listOf(
                GotrRune.Mind,
                GotrRune.Chaos,
                GotrRune.Death,
                GotrRune.Blood,
                GotrRune.Body,
                GotrRune.Cosmic,
                GotrRune.Nature,
                GotrRune.Law,
            )
    }
}

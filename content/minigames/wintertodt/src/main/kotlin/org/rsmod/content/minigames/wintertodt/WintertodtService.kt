package org.rsmod.content.minigames.wintertodt

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

private var Player.warmthVar by intVarBit(WintertodtService.WARMTH)
private var Player.poolVar by intVarBit(WintertodtService.POOL)
private var Player.killsVar by intVarp(WintertodtService.KILLS)

@Singleton
class WintertodtService
@Inject
constructor(
    private val players: PlayerList,
    private val clock: MapClock,
    private val locRepo: LocRepository,
    private val objs: ObjRepository,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
    private val events: EventBus,
) {
    val round = WintertodtRound { random.of(it) }

    private val inside = HashSet<Int>()
    private val brazierLocs = arrayOfNulls<LocInfo>(WintertodtRound.BRAZIERS)
    private var lastTick = -1

    fun isInside(player: Player): Boolean = player.slotId in inside

    fun enter(player: Player) {
        if (player.firemakingLvl < MIN_FIREMAKING) {
            player.mes("You need a Firemaking level of $MIN_FIREMAKING to enter the Wintertodt.")
            return
        }
        if (isInside(player)) return
        inside += player.slotId
        round.join(player.slotId)
        player.warmthVar = WARMTH_MAX
        player.ifOpenOverlay(OVERLAY, events)
        player.softTimer(TIMER, 1)
        teleport(player, WintertodtMap.ARENA_ENTRY)
        player.mes("The cold bites as you step into the Wintertodt's arena.")
    }

    fun leave(player: Player, toLobby: Boolean = true) {
        if (!inside.remove(player.slotId)) return
        round.leave(player.slotId)
        player.clearSoftTimer(TIMER)
        player.ifCloseOverlay(OVERLAY, events)
        player.warmthVar = 0
        if (toLobby) teleport(player, WintertodtMap.LOBBY_EXIT)
    }

    fun tick() {
        val cycle = clock.cycle
        if (cycle == lastTick) return
        lastTick = cycle
        apply(round.tick(cycle))
        if (cycle % REGEN_INTERVAL == 0) regenerate()
        if (cycle % OVERLAY_INTERVAL == 0) refreshOverlays()
    }

    fun light(player: Player, brazier: Int): Boolean = round.light(brazier, player.slotId).also { if (it) sync(brazier) }

    fun repair(player: Player, brazier: Int): Boolean = round.repair(brazier, player.slotId).also { if (it) sync(brazier) }

    fun feed(player: Player, brazier: Int, points: Int): Boolean = round.feed(brazier, player.slotId, points)

    fun warmth(player: Player): Int = player.warmthVar

    fun restore(player: Player, amount: Int) {
        player.warmthVar = (warmth(player) + amount).coerceAtMost(WARMTH_MAX)
    }

    fun warmPieces(player: Player): Int =
        player.worn.filterNotNull { worn -> WARM_ITEMS.any { worn.isType(it) } }.size.coerceAtMost(MAX_WARM_PIECES)

    fun poolOf(player: Player): Int = player.poolVar

    fun spendPool(player: Player, amount: Int) {
        player.poolVar = (poolOf(player) - amount).coerceAtLeast(0)
    }

    private fun apply(list: List<WintertodtEvent>) {
        for (event in list) {
            when (event) {
                WintertodtEvent.Started -> {
                    syncAll()
                    broadcast("The Wintertodt awakens! Light the braziers and keep them burning.")
                }
                is WintertodtEvent.Attack -> attack(event)
                is WintertodtEvent.Ended -> finish(event.points)
            }
        }
    }

    private fun attack(event: WintertodtEvent.Attack) {
        when (event.kind) {
            AttackKind.Cold -> {
                broadcast("The cold of the Wintertodt's attack chills you to the bone!")
                forEachInside { chill(it, COLD_LOSS) }
            }
            AttackKind.Snow -> {
                val victim = inside.randomOrNull()?.let { players[it] } ?: return
                val centre = victim.coords
                forEachInside { if (it.coords.chebyshevDistance(centre) <= 1) chill(it, SNOW_LOSS) }
                broadcast("A heavy snowfall batters the area around ${victim.displayName}!")
            }
            AttackKind.Extinguish -> {
                event.brazier?.let(::sync)
                broadcast("The Wintertodt snuffs out a brazier.")
            }
            AttackKind.Shatter -> {
                val index = event.brazier ?: return
                sync(index)
                forEachInside { if (WintertodtMap.BRAZIERS[index].chebyshevDistance(it.coords) <= 3) chill(it, SHATTER_LOSS) }
                broadcast("The Wintertodt shatters a brazier!")
            }
        }
    }

    private fun chill(player: Player, base: Int) {
        val loss = (base - PIECE_REDUCTION * warmPieces(player)).coerceAtLeast(MIN_LOSS)
        val left = warmth(player) - loss
        if (left > 0) {
            player.warmthVar = left
            return
        }
        player.mes("The cold overwhelms you and you are dragged out of the arena.")
        leave(player)
    }

    private fun regenerate() {
        forEachInside { player ->
            val gain = BASE_REGEN + warmPieces(player)
            player.warmthVar = (warmth(player) + gain).coerceAtMost(WARMTH_MAX)
        }
    }

    private fun finish(points: Map<Int, Int>) {
        broadcast("The Wintertodt has been subdued!")
        for ((id, earned) in points) {
            val player = players[id] ?: continue
            val rolls = WintertodtLoot.rolls(earned) { random.of(it) }
            if (rolls == 0) {
                player.mes("You didn't earn enough points to be rewarded. You need $MIN_REWARD_POINTS or more.")
                continue
            }
            player.statAdvance("stat.firemaking", player.firemakingLvl * REWARD_XP_PER_LEVEL.toDouble())
            player.poolVar = (poolOf(player) + rolls).coerceAtMost(WintertodtLoot.MAX_ROLLS)
            player.killsVar += 1
            player.mes("You earned $earned points and $rolls reward cart searches.")
        }
        syncAll()
    }

    private fun sync(index: Int) {
        val old = brazierLocs[index] ?: locRepo.findExact(WintertodtMap.BRAZIERS[index], LocShape.CentrepieceStraight)
        val symbol =
            when (round.brazier(index)) {
                BrazierState.Unlit -> "loc.wint_brazier"
                BrazierState.Lit -> "loc.wint_brazier_lit"
                BrazierState.Broken -> "loc.wint_brazier_broken"
            }
        val base = old ?: return
        locRepo.del(base, Int.MAX_VALUE)
        brazierLocs[index] = locRepo.add(base.coords, symbol, Int.MAX_VALUE, base.angle, base.shape)
    }

    private fun syncAll() {
        for (index in 0 until WintertodtRound.BRAZIERS) sync(index)
    }

    private fun refreshOverlays() {
        val script = "clientscript.[clientscript,wint_update]".asRSCM(RSCMType.CLIENTSCRIPT)
        val states = (0 until WintertodtRound.BRAZIERS).map { round.brazier(it).client }
        forEachInside { player ->
            player.runClientScript(
                script,
                round.pointsOf(player.slotId),
                round.energy.toInt(),
                PYRO_HIDDEN,
                PYRO_HIDDEN,
                PYRO_HIDDEN,
                PYRO_HIDDEN,
                states[0],
                states[1],
                states[2],
                states[3],
            )
        }
    }

    private fun broadcast(message: String) = forEachInside { it.mes(message) }

    private fun forEachInside(action: (Player) -> Unit) {
        for (id in inside.toList()) {
            val player = players[id]
            if (player == null) {
                inside.remove(id)
                round.leave(id)
                continue
            }
            action(player)
        }
    }

    private fun teleport(player: Player, dest: CoordGrid) {
        PathingEntityCommon.telejump(player, collision, dest)
    }

    companion object {
        const val TIMER = "timer.wintertodt"
        const val OVERLAY = "interface.wint_status"
        const val WARMTH = "varbit.wint_warmth"
        const val POOL = "varbit.wint_reward_pool"
        const val KILLS = "varp.total_wintertodt_kills"
        const val WARMTH_MAX = 1000
        const val MIN_FIREMAKING = 50
        const val MIN_REWARD_POINTS = WintertodtRound.MIN_POINTS
        const val REWARD_XP_PER_LEVEL = 100
        const val COLD_LOSS = 90
        const val SNOW_LOSS = 220
        const val SHATTER_LOSS = 220
        const val MIN_LOSS = 20
        const val PIECE_REDUCTION = 15
        const val MAX_WARM_PIECES = 4
        const val BASE_REGEN = 8
        const val REGEN_INTERVAL = 10
        const val OVERLAY_INTERVAL = 2
        const val PYRO_HIDDEN = 1

        val WARM_ITEMS =
            listOf(
                "obj.pyromancer_hood",
                "obj.pyromancer_top",
                "obj.pyromancer_bottom",
                "obj.pyromancer_boots",
                "obj.pyromancer_gloves",
            )
    }
}

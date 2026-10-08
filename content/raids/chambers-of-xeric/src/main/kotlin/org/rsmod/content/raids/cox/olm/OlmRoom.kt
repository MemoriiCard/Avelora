package org.rsmod.content.raids.cox.olm

import org.rsmod.api.player.disableOverheadPrayers
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statDrain
import org.rsmod.api.player.stat.statSub
import org.rsmod.content.raids.cox.layout.CoxCell
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.layout.CoxRoomType
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.content.raids.cox.room.CoxNpcStats
import org.rsmod.content.raids.cox.room.CoxRoomController
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

/**
 * The Great Olm. Each phase raises a head and two claws on one side of the arena; once both claws
 * are down the head sinks, crystals rain, and everything rises again on the opposite side. The
 * head runs a fixed twelve-step rotation of standard attacks and the claws' specials.
 */
class OlmRoom(
    raid: CoxRaid,
    room: CoxRoom,
    services: CoxRoomServices,
    private val finished: () -> Unit,
) : CoxRoomController(raid, room, services) {
    private enum class State {
        Idle,
        Rising,
        Active,
        Sinking,
    }

    private val phaseCount = raid.scaling.olmPhases
    private val clock: Int
        get() = services.cycle

    private var state = State.Idle
    private var stateTicks = 0
    private var phase = -1
    private var side = OlmSide.West
    private var powers: List<Set<OlmPower>> = emptyList()
    private val used = mutableSetOf<OlmAttack>()

    private var head: Npc? = null
    private var leftHand: Npc? = null
    private var rightHand: Npc? = null
    private var leftDown = false
    private var rightDown = false
    private var regrowLeftAt = -1
    private var regrowRightAt = -1
    private val regrown = mutableSetOf<Npc>()
    private var regrowthStarted = false

    private var ticksInPhase = 0
    private var stepIndex = 0
    private var catchUp = false
    private var facing = OlmZone.Middle
    private var rangedStyle = false
    private var clenchedUntil = -1
    private val leftDamage = ArrayDeque<Pair<Int, Int>>()
    private var healingFrom = -1
    private var healingUntil = -1
    private val lastDamaged = mutableMapOf<Npc, Int>()

    private val pools = mutableListOf<Pool>()
    private val burning = mutableMapOf<Player, Burn>()
    private val bombs = mutableListOf<Bomb>()
    private val bursts = mutableListOf<Burst>()
    private val walls = mutableListOf<Wall>()
    private val bolts = mutableListOf<Bolt>()
    private val swaps = mutableListOf<Swap>()
    private val sprayTargets = mutableMapOf<Player, Int>()
    private val dripUntil = mutableMapOf<Player, Int>()
    private val crystalMarks = mutableMapOf<Player, CrystalMark>()
    private val queuedCrystals = ArrayDeque<Pair<Int, () -> Unit>>()
    private var siphon: Siphon? = null

    val engagedAndActive: Boolean
        get() = engaged && !cleared

    override fun spawn() {}

    override fun inside(coords: CoordGrid): Boolean =
        raid.inOlmRoom(coords) && raid.olmStatic(coords).z >= ARENA_MIN_Z

    override fun awardsPoints(npc: Npc): Boolean = !regrowthStarted && npc !in regrown

    override fun onEngage(first: Player) {
        powers = OlmRules.powersByPhase(phaseCount) { services.random.of(it) }
        side = if (services.random.of(2) == 0) OlmSide.West else OlmSide.East
        startPhase(0)
    }

    override fun onTick() {
        stateTicks++
        when (state) {
            State.Idle -> {}
            State.Rising -> if (stateTicks >= RISE_TICKS) beginActive()
            State.Active -> activeTick()
            State.Sinking -> if (stateTicks >= SINK_TICKS) finishPhase()
        }
        tickEffects()
    }

    override fun onKilled(npc: Npc, hero: Player, dropCoords: CoordGrid) {
        when {
            npc === head && phase >= phaseCount - 1 -> clear()
            npc === leftHand -> handDown(left = true)
            npc === rightHand -> handDown(left = false)
        }
    }

    override fun onNpcRemoved(npc: Npc) {
        if (npc === head) head = null
        if (npc === leftHand) leftHand = null
        if (npc === rightHand) rightHand = null
        regrown.remove(npc)
        lastDamaged.remove(npc)
    }

    override fun onCleared() {
        for (player in playersInRoom()) player.mes("<col=ef1020>The Great Olm has been defeated!</col>")
        finished()
    }

    /** Applies the claw and head damage rules to a hit from a player; returns the damage dealt. */
    fun incoming(npc: Npc, type: HitType, damage: Int): Int {
        if (state != State.Active || damage <= 0) return 0
        var dealt = damage
        when {
            npc === head -> {
                if (type != HitType.Ranged) dealt = dealt * OFF_STYLE_PERCENT / 100
                if (phase < phaseCount - 1) dealt = dealt.coerceAtMost(npc.hitpoints - 1)
            }
            npc === rightHand -> if (type != HitType.Magic) dealt = dealt * OFF_STYLE_PERCENT / 100
            npc === leftHand -> {
                if (clock < clenchedUntil) return 0
                if (type != HitType.Melee) dealt = dealt * OFF_STYLE_PERCENT / 100
                if (clock in healingFrom..healingUntil) {
                    heal(npc, dealt)
                    return 0
                }
                recordLeftDamage(npc, dealt)
            }
        }
        lastDamaged[npc] = clock
        return dealt.coerceAtLeast(0)
    }

    private fun startPhase(index: Int) {
        phase = index
        used.clear()
        stepIndex = 0
        catchUp = false
        ticksInPhase = 0
        facing = OlmZone.Middle
        leftDown = false
        rightDown = false
        regrowLeftAt = -1
        regrowRightAt = -1
        regrowthStarted = false
        regrown.clear()
        leftDamage.clear()
        clenchedUntil = -1
        healingFrom = -1
        healingUntil = -1
        siphon = null
        spawnParts()
        state = State.Rising
        stateTicks = 0
        val message =
            when {
                index >= phaseCount - 1 -> "The Great Olm is giving its all! This is its final stand!"
                index == phaseCount - 2 -> "The Great Olm is nearing the end... it rises with every power!"
                else -> "The Great Olm rises with the power of ${powers[index].single().name.lowercase()}!"
            }
        for (player in playersInRoom()) player.mes("<col=ef1020>$message</col>")
    }

    private fun spawnParts() {
        val layout = OlmRules.layout(side)
        val final = phase >= phaseCount - 1
        val enraged = phase >= phaseCount - 2
        val headSeq = if (enraged) "seq.olm_head_spawn_enraged" else "seq.olm_head_spawn"
        head =
            spawnPart(HEAD, layout.head, HEAD_STATS, scaling.olmHeadHitpoints).also {
                it.anim(headSeq)
            }
        if (final) return
        leftHand =
            spawnPart(LEFT_HAND, layout.leftHand, LEFT_STATS, scaling.olmHandHitpoints).also {
                it.anim("seq.olm_hand_left_spawn")
            }
        rightHand =
            spawnPart(RIGHT_HAND, layout.rightHand, RIGHT_STATS, scaling.olmHandHitpoints).also {
                it.anim("seq.olm_hand_right_spawn")
            }
    }

    private val scaling
        get() = raid.scaling

    private fun spawnPart(type: String, pos: OlmPos, stats: CoxNpcStats, hitpoints: Int): Npc {
        val npc = spawnAt(type, world(pos), stats, required = false, hitpoints = hitpoints)
        npc.movementLocked = true
        return npc
    }

    private fun beginActive() {
        state = State.Active
        stateTicks = 0
        head?.anim(idleSeq())
    }

    private fun activeTick() {
        ticksInPhase++
        val now = clock
        if (regrowLeftAt in 0..now) regrow(left = true)
        if (regrowRightAt in 0..now) regrow(left = false)
        if (ticksInPhase % OlmRules.ATTACK_RATE == 0) headStep()
        if (phase >= phaseCount - 1 && ticksInPhase % SIPHON_RATE == 0) startSiphon()
    }

    private fun headStep() {
        val step = OlmRules.ROTATION[stepIndex % OlmRules.ROTATION.size]
        stepIndex++
        val specials = OlmRules.hasSpecials(phase, phaseCount) && handsAlive() && !clenched()
        when (step) {
            OlmStep.Standard -> standard()
            OlmStep.Empty ->
                if (catchUp) {
                    catchUp = false
                    standard()
                }
            OlmStep.CrystalBurst -> if (specials) crystalBurst() else standard()
            OlmStep.Lightning -> if (specials) lightning() else standard()
            OlmStep.Swap -> if (specials) swap() else standard()
        }
        if (phase >= phaseCount - 1) markFinalCrystal()
    }

    private fun handsAlive() = leftHand != null || rightHand != null

    private fun clenched() = clock < clenchedUntil

    private fun standard() {
        val target = chooseTarget() ?: return
        val roll = services.random.of(if (phase >= phaseCount - 1) OlmRules.FINAL_POWER_CHANCE else OlmRules.POWER_CHANCE)
        val options = OlmRules.availableAttacks(powers[phase], used)
        when {
            roll == 0 && options.isNotEmpty() -> power(options[services.random.of(options.size)], target)
            services.random.of(OlmRules.SPHERE_CHANCE) == 0 -> sphere(target)
            else -> orb(target)
        }
    }

    private fun chooseTarget(): Player? {
        val players = playersInRoom()
        if (players.isEmpty()) return null
        val counts = OlmZone.entries.associateWith { zone -> players.count { zoneOf(it) == zone } }
        val zone = OlmRules.busiestZone(counts, facing) ?: return null
        if (zone != facing) {
            turnTo(zone)
            catchUp = true
            return null
        }
        return players.filter { zoneOf(it) == zone }.random()
    }

    private fun zoneOf(player: Player) = OlmRules.zoneOf(raid.olmStatic(player.coords).z, side)

    private fun turnTo(zone: OlmZone) {
        facing = zone
        head?.anim(
            when (zone) {
                OlmZone.Left -> "seq.olm_head_turn_left"
                OlmZone.Right -> "seq.olm_head_turn_right"
                OlmZone.Middle -> idleSeq()
            }
        )
    }

    private fun idleSeq(): String {
        val enraged = phase >= phaseCount - 2
        return when (facing) {
            OlmZone.Left -> if (enraged) "seq.olm_head_idle_left_enraged" else "seq.olm_head_idle_left"
            OlmZone.Right -> if (enraged) "seq.olm_head_idle_right_enraged" else "seq.olm_head_idle_right"
            OlmZone.Middle -> if (enraged) "seq.olm_head_idle_front_enraged" else "seq.olm_head_idle_front"
        }
    }

    private fun attackSeq(): String =
        when (facing) {
            OlmZone.Left -> "seq.olm_head_attack_acid_left"
            OlmZone.Right -> "seq.olm_head_attack_acid_right"
            OlmZone.Middle -> "seq.olm_head_attack_acid_front"
        }

    private fun maxHit() = OlmRules.maxHit(phase, phaseCount, raid.challengeMode)

    private fun orb(target: Player) {
        val ranged = rangedStyle
        val damage = services.random.of(0, maxHit())
        val travel = if (ranged) "spotanim.olm_weak_range_proj" else "spotanim.olm_weak_mage_proj"
        val impact = if (ranged) "spotanim.olm_weak_range_impact" else "spotanim.olm_weak_mage_impact"
        head?.anim(attackSeq())
        val prayer = if (ranged) OlmSphere.Ranged else OlmSphere.Magic
        launch(travel, target) {
            services.spotanimAt(impact, target.coords)
            val dealt = if (target.vars[prayer.prayer] > 0) damage * OlmRules.PRAYER_LEAK_PERCENT / 100 else damage
            hurt(target, dealt)
        }
        if (services.random.of(STYLE_SWITCH) == 0) rangedStyle = !rangedStyle
    }

    private fun sphere(target: Player) {
        val sphere = OlmSphere.entries[services.random.of(OlmSphere.entries.size)]
        val wasPraying = target.vars[sphere.prayer] > 0
        head?.anim(attackSeq())
        val travel =
            when (sphere) {
                OlmSphere.Melee -> "spotanim.olm_weak_melee_proj"
                OlmSphere.Ranged -> "spotanim.olm_weak_range_proj"
                OlmSphere.Magic -> "spotanim.olm_weak_mage_proj"
            }
        target.mes("<col=ef1020>The Great Olm fires a ${sphere.name.lowercase()} sphere at you!</col>")
        launch(travel, target, SPHERE_TRAVEL) {
            val praying = target.vars[sphere.prayer] > 0
            when {
                praying && wasPraying -> {
                    target.disableOverheadPrayers()
                    target.statSub("stat.prayer", 0, 50)
                    target.mes("The sphere strips your prayer!")
                }
                praying -> target.mes("You switch prayers just in time and the sphere breaks.")
                else -> hurt(target, target.hitpoints / 2)
            }
        }
    }

    private fun power(attack: OlmAttack, target: Player) {
        used += attack
        head?.anim(attackSeq())
        when (attack) {
            OlmAttack.AcidSpray -> acidSpray(target)
            OlmAttack.AcidDrip -> {
                dripUntil[target] = clock + DRIP_TICKS
                target.mes("<col=ef1020>The Great Olm covers you in acid!</col>")
            }
            OlmAttack.Burn -> ignite(target)
            OlmAttack.FireWall -> fireWall(target)
            OlmAttack.FallingCrystals -> {
                crystalMarks[target] = CrystalMark(clock + CRYSTAL_MARK_TICKS)
                target.mes("<col=ef1020>The Great Olm is targeting you with falling crystals!</col>")
            }
            OlmAttack.CrystalBombs -> crystalBombs()
        }
    }

    private fun acidSpray(target: Player) {
        val at = pos(target)
        val from = OlmRules.layout(side).head
        val step = if (at.x >= from.x) 1 else -1
        var x = from.x + step * HEAD_REACH
        while (x != at.x + step) {
            for (dz in -1..1) addPool(OlmPos(x, at.z + dz))
            x += step
        }
    }

    private fun addPool(at: OlmPos) {
        if (pools.any { it.at == at }) return
        pools += Pool(at, clock + POOL_TICKS)
        services.boss.locRepo.add(
            world(at),
            "loc.olm_acid_pool",
            POOL_TICKS,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
    }

    private fun ignite(target: Player) {
        burning[target] = Burn(BURN_HITS, clock + BURN_RATE)
        head?.say("Burn with me!")
    }

    private fun fireWall(target: Player) {
        val z = pos(target).z
        walls += Wall(z - WALL_GAP, z + WALL_GAP, clock + WALL_TICKS)
        for (player in playersInRoom()) {
            val at = pos(player)
            if (at.z == z - WALL_GAP || at.z == z + WALL_GAP) hurt(player, services.random.of(8, 10))
        }
    }

    private fun crystalBombs() {
        val players = playersInRoom().shuffled().take(OlmRules.bombCount(raid.scaling.partySize))
        for (player in players) {
            val at = pos(player)
            bombs += Bomb(at, clock + BOMB_TICKS)
            services.boss.locRepo.add(
                world(at),
                "loc.olm_crystal_bomb",
                BOMB_TICKS,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        }
    }

    private fun crystalBurst() {
        for (player in playersInRoom()) {
            val at = pos(player)
            bursts += Burst(at, clock + BURST_TICKS)
            services.boss.locRepo.add(
                world(at),
                "loc.olm_crystal_attack_small",
                BURST_TICKS,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        }
        leftHand?.anim("seq.olm_hand_left_cast_earthquake")
    }

    private fun lightning() {
        val players = playersInRoom()
        val lanes = (players.map { pos(it).x } + List(EXTRA_LANES) { ARENA_MIN_X + services.random.of(ARENA_WIDTH) }).distinct()
        val north = services.random.of(2) == 0
        for (x in lanes.take(MAX_LANES)) bolts += Bolt(x, if (north) ARENA_MIN_Z else ARENA_MAX_Z, north)
        rightHand?.anim("seq.olm_hand_left_cast_earthshock")
    }

    private fun swap() {
        val players = playersInRoom()
        if (players.size == 1) {
            val player = players.single()
            swaps += Swap(player, null, pos(player), findPortal(player), clock + SWAP_TICKS)
            return
        }
        for ((index, pair) in OlmRules.pairUp(players) { services.random.of(it) }.withIndex()) {
            val colour = "spotanim.olm_playerswap_${index % PAIR_COLOURS}"
            swaps += Swap(pair.first, pair.second, pos(pair.first), pos(pair.second), clock + SWAP_TICKS)
            services.spotanimAt(colour, pair.first.coords)
            services.spotanimAt(colour, pair.second.coords)
        }
        if (phase == phaseCount - 2) {
            healingFrom = clock + SWAP_TICKS
            healingUntil = healingFrom + HEAL_WINDOW
        }
    }

    private fun findPortal(player: Player): OlmPos {
        val here = pos(player)
        repeat(PORTAL_TRIES) {
            val candidate =
                OlmPos(
                    (here.x + services.random.of(-PORTAL_RANGE, PORTAL_RANGE)).coerceIn(ARENA_MIN_X, ARENA_MAX_X),
                    (here.z + services.random.of(-PORTAL_RANGE, PORTAL_RANGE)).coerceIn(ARENA_MIN_Z, ARENA_MAX_Z),
                )
            if (!services.collision.isWalkBlocked(world(candidate))) return candidate
        }
        return here
    }

    private fun startSiphon() {
        val players = playersInRoom()
        if (players.isEmpty()) return
        val anchor = pos(players.random())
        val tiles =
            List(SIPHON_TILES) {
                OlmPos(
                    (anchor.x + services.random.of(-2, 2)).coerceIn(ARENA_MIN_X, ARENA_MAX_X),
                    (anchor.z + services.random.of(-2, 2)).coerceIn(ARENA_MIN_Z, ARENA_MAX_Z),
                )
            }
        siphon = Siphon(tiles, clock + SIPHON_TICKS)
        for (tile in tiles) services.spotanimAt("spotanim.olm_healme_spotanim", world(tile))
        head?.say("Life Siphon!")
        for (player in players) player.mes("<col=0070ff>Stand on a blue tile to avoid the life siphon!</col>")
    }

    private fun markFinalCrystal() {
        val players = playersInRoom()
        if (players.isEmpty()) return
        val target = players.random()
        val at = pos(target)
        services.spotanimAt("spotanim.olm_crystalrock_falling", world(at))
        queuedCrystals += clock + CRYSTAL_DELAY to { dropCrystal(at) }
    }

    private fun dropCrystal(at: OlmPos) {
        services.spotanimAt("spotanim.olm_crystal_explode", world(at))
        for (player in playersInRoom()) {
            val damage = OlmRules.crystalDamage(OlmRules.distance(pos(player), at)) { a, b -> services.random.of(a, b) }
            hurt(player, damage)
        }
    }

    private fun tickEffects() {
        val now = clock
        pools.removeAll { it.expires <= now }
        if (pools.isNotEmpty()) {
            for (player in playersInRoom()) {
                if (pools.any { it.at == pos(player) }) hurt(player, services.random.of(3, 6))
            }
        }
        val dripIterator = dripUntil.entries.iterator()
        while (dripIterator.hasNext()) {
            val (player, until) = dripIterator.next()
            if (until <= now || player !in playersInRoom()) {
                dripIterator.remove()
                continue
            }
            addPool(pos(player))
        }
        tickBurning(now)
        tickBombs(now)
        tickBursts(now)
        tickWalls(now)
        tickBolts()
        tickSwaps(now)
        tickCrystalMarks(now)
        tickSiphon(now)
        while (queuedCrystals.isNotEmpty() && queuedCrystals.first().first <= now) queuedCrystals.removeFirst().second()
    }

    private fun tickBurning(now: Int) {
        if (burning.isEmpty()) return
        for ((player, burn) in burning.entries.toList()) {
            if (player !in playersInRoom()) {
                burning.remove(player)
                continue
            }
            if (burn.next > now) continue
            hurt(player, BURN_DAMAGE)
            for (stat in COMBAT_STATS) player.statDrain(stat, BURN_DRAIN, 0)
            burn.remaining--
            burn.next = now + BURN_RATE
            if (burn.remaining <= 0) {
                burning.remove(player)
                continue
            }
            val neighbour =
                playersInRoom().firstOrNull {
                    it !== player && it !in burning && it.coords.chebyshevDistance(player.coords) <= 1
                }
            if (neighbour != null) {
                burning.remove(player)
                burning[neighbour] = Burn(BURN_HITS, now + BURN_RATE)
                neighbour.mes("<col=ef1020>I will burn with you.</col>")
            }
        }
    }

    private fun tickBombs(now: Int) {
        val due = bombs.filter { it.explodesAt <= now }
        bombs.removeAll(due)
        for (bomb in due) {
            services.spotanimAt("spotanim.olm_crystal_explode", world(bomb.at))
            for (player in playersInRoom()) {
                hurt(player, OlmRules.bombDamage(OlmRules.distance(pos(player), bomb.at)))
            }
        }
    }

    private fun tickBursts(now: Int) {
        val due = bursts.filter { it.explodesAt <= now }
        bursts.removeAll(due)
        for (burst in due) {
            services.spotanimAt("spotanim.olm_crystalwave_shatter", world(burst.at))
            for (player in playersInRoom()) {
                if (pos(player) == burst.at) hurt(player, services.random.of(BURST_MIN, BURST_MAX))
            }
        }
    }

    private fun tickWalls(now: Int) {
        val due = walls.filter { it.closesAt <= now }
        walls.removeAll(due)
        for (wall in due) {
            for (player in playersInRoom()) {
                val z = pos(player).z
                if (z > wall.low && z < wall.high) hurt(player, services.random.of(WALL_MIN, WALL_MAX))
            }
        }
    }

    private fun tickBolts() {
        val iterator = bolts.iterator()
        while (iterator.hasNext()) {
            val bolt = iterator.next()
            repeat(BOLT_SPEED) { bolt.advance() }
            if (bolt.z !in ARENA_MIN_Z..ARENA_MAX_Z) {
                iterator.remove()
                continue
            }
            services.spotanimAt("spotanim.olm_shockwave", world(OlmPos(bolt.x, bolt.z)))
            for (player in playersInRoom()) {
                val at = pos(player)
                if (at.x == bolt.x && kotlin.math.abs(at.z - bolt.z) <= BOLT_SPEED) {
                    hurt(player, services.random.of(BOLT_MIN, BOLT_MAX))
                    player.disableOverheadPrayers()
                }
            }
        }
    }

    private fun tickSwaps(now: Int) {
        val due = swaps.filter { it.resolveAt <= now }
        swaps.removeAll(due)
        for (swap in due) {
            val distance = OlmRules.distance(swap.fromA, swap.fromB)
            val damage = OlmRules.swapDamage(distance)
            val a = swap.a
            val b = swap.b
            if (a in playersInRoom()) {
                teleport(a, swap.fromB)
                hurt(a, damage)
            }
            if (b != null && b in playersInRoom()) {
                teleport(b, swap.fromA)
                hurt(b, damage)
            }
        }
    }

    private fun tickCrystalMarks(now: Int) {
        val iterator = crystalMarks.entries.iterator()
        while (iterator.hasNext()) {
            val (player, mark) = iterator.next()
            if (mark.until <= now || player !in playersInRoom()) {
                iterator.remove()
                continue
            }
            val at = pos(player)
            services.spotanimAt("spotanim.olm_crystalrock_falling", world(at))
            queuedCrystals += now + CRYSTAL_DELAY to { dropCrystal(at) }
        }
    }

    private fun tickSiphon(now: Int) {
        val active = siphon ?: return
        if (active.resolveAt > now) return
        siphon = null
        var total = 0
        for (player in playersInRoom()) {
            if (active.tiles.contains(pos(player))) continue
            val damage = services.random.of(SIPHON_MIN, SIPHON_MAX)
            hurt(player, damage)
            total += damage
        }
        head?.let { heal(it, OlmRules.siphonHeal(total)) }
    }

    private fun handDown(left: Boolean) {
        if (left) leftDown = true else rightDown = true
        if (leftDown && rightDown) {
            endPhase()
            return
        }
        if (phase == phaseCount - 2) {
            if (left) regrowLeftAt = clock + OlmRules.REGROW_TICKS else regrowRightAt = clock + OlmRules.REGROW_TICKS
        }
    }

    private fun regrow(left: Boolean) {
        val layout = OlmRules.layout(side)
        if (left) regrowLeftAt = -1 else regrowRightAt = -1
        if (left && !leftDown || !left && !rightDown) return
        regrowthStarted = true
        val npc =
            if (left) {
                spawnPart(LEFT_HAND, layout.leftHand, LEFT_STATS, scaling.olmHandHitpoints)
            } else {
                spawnPart(RIGHT_HAND, layout.rightHand, RIGHT_STATS, scaling.olmHandHitpoints)
            }
        regrown += npc
        if (left) {
            leftHand = npc
            leftDown = false
        } else {
            rightHand = npc
            rightDown = false
        }
    }

    private fun endPhase() {
        state = State.Sinking
        stateTicks = 0
        for (player in playersInRoom()) player.mes("<col=ef1020>The Great Olm sinks beneath the ground... watch out for falling crystals!</col>")
        val players = playersInRoom()
        if (players.isEmpty()) return
        repeat(SINK_CRYSTALS) { index ->
            val target = pos(players.random())
            val delay = index + 1
            services.boss.worldQueues.add(delay) {
                services.spotanimAt("spotanim.olm_crystalrock_falling", world(target))
                queuedCrystals += clock + CRYSTAL_DELAY to { dropCrystal(target) }
            }
        }
    }

    private fun finishPhase() {
        for (npc in listOfNotNull(head, leftHand, rightHand)) {
            if (npc.isSlotAssigned) services.npcRepo.del(npc, Int.MAX_VALUE)
        }
        head = null
        leftHand = null
        rightHand = null
        clearEffects()
        val next = phase + 1
        side = side.opposite
        if (next < phaseCount) startPhase(next)
    }

    private fun clearEffects() {
        pools.clear()
        burning.clear()
        bombs.clear()
        bursts.clear()
        walls.clear()
        bolts.clear()
        swaps.clear()
        dripUntil.clear()
        crystalMarks.clear()
        siphon = null
    }

    private fun recordLeftDamage(npc: Npc, dealt: Int) {
        val now = clock
        leftDamage.addLast(now to dealt)
        while (leftDamage.isNotEmpty() && now - leftDamage.first().first > OlmRules.CLENCH_WINDOW) {
            leftDamage.removeFirst()
        }
        val total = leftDamage.sumOf { it.second }
        if (rightHand != null && OlmRules.clenches(total, scaling.olmHandHitpoints)) {
            clenchedUntil = now + OlmRules.CLENCH_TICKS
            leftDamage.clear()
            npc.anim("seq.olm_hand_left_cast_stun")
        }
    }

    private fun heal(npc: Npc, amount: Int) {
        npc.hitpoints = (npc.hitpoints + amount).coerceAtMost(npc.baseHitpointsLvl)
    }

    private fun launch(spotanim: String, target: Player, extraDelay: Int = 0, onLand: () -> Unit) {
        val from = head?.coords?.translate(HEAD_CENTRE, HEAD_CENTRE) ?: target.coords
        val resolve = {
            if (target in playersInRoom()) onLand()
        }
        services.lob(spotanim, from, target.coords) {
            if (extraDelay > 0) services.boss.worldQueues.add(extraDelay) { resolve() } else resolve()
        }
    }

    private fun hurt(player: Player, damage: Int) {
        if (damage <= 0 || player.hitpoints <= 0) return
        player.queueHit(1, HitType.Typeless, damage, services.boss.playerHitModifier)
    }

    private fun teleport(player: Player, to: OlmPos) {
        org.rsmod.game.entity.util.PathingEntityCommon.telejump(player, services.collision, world(to))
    }

    private fun pos(player: Player): OlmPos {
        val static = raid.olmStatic(player.coords)
        return OlmPos(static.x, static.z)
    }

    private fun world(at: OlmPos): CoordGrid = raid.olmCoords(CoordGrid(at.x, at.z, 0))

    private class Pool(val at: OlmPos, val expires: Int)

    private class Burn(var remaining: Int, var next: Int)

    private class Bomb(val at: OlmPos, val explodesAt: Int)

    private class Burst(val at: OlmPos, val explodesAt: Int)

    private class Wall(val low: Int, val high: Int, val closesAt: Int)

    private class Bolt(val x: Int, var z: Int, val north: Boolean) {
        fun advance() {
            z += if (north) 1 else -1
        }
    }

    private class Swap(
        val a: Player,
        val b: Player?,
        val fromA: OlmPos,
        val fromB: OlmPos,
        val resolveAt: Int,
    )

    private class CrystalMark(val until: Int)

    private class Siphon(val tiles: List<OlmPos>, val resolveAt: Int)

    companion object {
        const val HEAD = "npc.olm_head"
        const val LEFT_HAND = "npc.olm_hand_left"
        const val RIGHT_HAND = "npc.olm_hand_right"

        val ROOM = CoxRoom(CoxRoomType.OlmEntrance, CoxCell(0, 0), null, null)

        val HEAD_STATS = CoxNpcStats(hitpoints = 800, attack = 250, strength = 250, defence = 150, ranged = 250, magic = 250)
        val LEFT_STATS = CoxNpcStats(hitpoints = 600, attack = 250, strength = 250, defence = 175, ranged = 250, magic = 175)
        val RIGHT_STATS = CoxNpcStats(hitpoints = 600, attack = 1, strength = 1, defence = 175, ranged = 1, magic = 87)

        const val ARENA_MIN_Z = 5730
        const val ARENA_MAX_Z = 5748
        const val ARENA_MIN_X = 3224
        const val ARENA_MAX_X = 3234
        private const val ARENA_WIDTH = ARENA_MAX_X - ARENA_MIN_X + 1

        private const val RISE_TICKS = 8
        private const val SINK_TICKS = 12
        private const val SINK_CRYSTALS = 8
        private const val OFF_STYLE_PERCENT = 34
        private const val STYLE_SWITCH = 5
        private const val SPHERE_TRAVEL = 2
        private const val HEAD_REACH = 3
        private const val HEAD_CENTRE = 2
        private const val POOL_TICKS = 16
        private const val DRIP_TICKS = 14
        private const val BURN_HITS = 6
        private const val BURN_RATE = 4
        private const val BURN_DAMAGE = 5
        private const val BURN_DRAIN = 2
        private const val WALL_GAP = 3
        private const val WALL_TICKS = 8
        private const val WALL_MIN = 50
        private const val WALL_MAX = 65
        private const val BOMB_TICKS = 6
        private const val BURST_TICKS = 5
        private const val BURST_MIN = 25
        private const val BURST_MAX = 40
        private const val EXTRA_LANES = 2
        private const val MAX_LANES = 5
        private const val BOLT_SPEED = 2
        private const val BOLT_MIN = 12
        private const val BOLT_MAX = 22
        private const val SWAP_TICKS = 5
        private const val PAIR_COLOURS = 4
        private const val HEAL_WINDOW = 8
        private const val PORTAL_RANGE = 8
        private const val PORTAL_TRIES = 12
        private const val CRYSTAL_MARK_TICKS = 11
        private const val CRYSTAL_DELAY = 2
        private const val SIPHON_RATE = 40
        private const val SIPHON_TICKS = 6
        private const val SIPHON_TILES = 2
        private const val SIPHON_MIN = 8
        private const val SIPHON_MAX = 18

        private val COMBAT_STATS =
            listOf("stat.attack", "stat.strength", "stat.defence", "stat.ranged", "stat.magic")
    }
}

package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.ScriptContext

class IceDemonScript
@Inject
constructor(
    deps: BossDeps,
    private val points: CoxDamagePoints,
    private val services: CoxRoomServices,
    private val raids: CoxRaids,
) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                val fire = hit.secondaryType()?.internalName?.contains("fire") == true
                hit.damage =
                    if (hit.type == HitType.Magic && fire) hit.damage * FIRE_PERCENT / 100
                    else hit.damage * RESIST_PERCENT / 100
            },
            onHit = { points.award(this) },
        )
        deps.extensionRegistry.register(ATTACK) { ctx -> attack(ctx.npc, ctx.target) }
        onOpLoc1(IceDemonRoom.SAPLING) { chop(it.loc) }
        onOpLoc1(IceDemonRoom.BRAZIER_UNLIT) { light(it.loc) }
        onOpLoc1(IceDemonRoom.BRAZIER_LIT) { fuel(it.loc) }
    }

    private fun attack(demon: Npc, target: Player) {
        val style =
            when {
                target.vars[PROTECT_FROM_MISSILES] > 0 -> HitType.Ranged
                target.vars[PROTECT_FROM_MAGIC] > 0 -> HitType.Magic
                services.random.of(0, 1) == 0 -> HitType.Ranged
                else -> HitType.Magic
            }
        val tile = target.coords
        demon.anim(CAST_SEQ)
        val travel = if (style == HitType.Ranged) SNOWBALL_TRAVEL else BURST_TRAVEL
        val impact = if (style == HitType.Ranged) SNOWBALL_IMPACT else BURST_IMPACT
        services.lob(travel, demon.coords.translate(1, 1), tile) {
            services.spotanimAt(impact, tile)
            for (player in services.players) {
                if (player.coords.level != tile.level) continue
                if (player.coords.chebyshevDistance(tile) > 1) continue
                services.strike(demon, player, style)
            }
        }
    }

    private suspend fun ProtectedAccess.chop(sapling: BoundLocInfo) {
        val room = roomFor(sapling) ?: return
        if (room.thawed) return
        if (!hasAxe()) {
            mes("You need an axe to chop this sapling.")
            return
        }
        while (true) {
            if (inv.isFull()) {
                mes("Your inventory is too full to hold any more kindling.")
                break
            }
            anim(CHOP_SEQ)
            delay(CHOP_DELAY)
            val pieces = random.of(0, player.woodcuttingLvl / KINDLING_LEVEL_STEP).coerceAtLeast(1)
            val space = MAX_KINDLING - inv.count(IceDemonRoom.KINDLING)
            if (space <= 0) {
                mes("You can't carry any more kindling.")
                break
            }
            invAdd(inv, IceDemonRoom.KINDLING, count = minOf(pieces, space), strict = false)
            if (room.saplingDepletes()) {
                deps.locRepo.change(sapling, IceDemonRoom.SAPLING_STUMP, SAPLING_RESPAWN)
                break
            }
        }
        resetAnim()
    }

    private suspend fun ProtectedAccess.light(loc: BoundLocInfo) {
        val room = roomFor(loc) ?: return
        val brazier = room.brazierAt(loc.coords) ?: return
        if (!playerContainsObj("obj.tinderbox")) {
            mes("You need a tinderbox to light the brazier.")
            return
        }
        if (inv.count(IceDemonRoom.KINDLING) == 0) {
            mes("You need some kindling to light the brazier.")
            return
        }
        anim(LIGHT_SEQ)
        delay(LIGHT_DELAY)
        val chance = LIGHT_MIN + (player.firemakingLvl - 1) * (LIGHT_MAX - LIGHT_MIN) / 98
        if (random.of(1, 100) > chance) {
            mes("You fail to light the brazier.")
            return
        }
        if (invDel(inv, IceDemonRoom.KINDLING).failure) return
        room.light(player, brazier)
        mes("The brazier catches alight.")
    }

    private fun ProtectedAccess.fuel(loc: BoundLocInfo) {
        val room = roomFor(loc) ?: return
        val brazier = room.brazierAt(loc.coords) ?: return
        val kindling = inv.count(IceDemonRoom.KINDLING)
        if (kindling == 0) {
            mes("You have no kindling to add.")
            return
        }
        if (invDel(inv, IceDemonRoom.KINDLING, count = kindling).failure) return
        anim(FUEL_SEQ)
        room.fuel(player, brazier, kindling)
    }

    private fun ProtectedAccess.roomFor(loc: BoundLocInfo): IceDemonRoom? {
        val raid = raids.containing(player) ?: return null
        return raid.roomAt(loc.coords)?.let(raid::controllerOf) as? IceDemonRoom
    }

    private fun ProtectedAccess.hasAxe(): Boolean {
        val worn = player.righthand?.let { getInvObj(it) }
        if (worn != null && worn.isContentType(AXE)) return true
        return inv.any { it != null && getInvObj(it).isContentType(AXE) }
    }

    override val spec: BossSpec =
        boss(IceDemonRoom.DEMON) {
            stats(attackRate = 3)
            val blast = ability("blast") { include(external(ATTACK)) }
            phase("fight") { weightedSelectorRandom { +random(blast, weight = 1) } }
        }

    private companion object {
        const val ATTACK = "cox.icedemon_attack"
        const val AXE = "content.woodcutting_axe"
        const val PROTECT_FROM_MISSILES = "varbit.prayer_protectfrommissiles"
        const val PROTECT_FROM_MAGIC = "varbit.prayer_protectfrommagic"
        const val CAST_SEQ = "seq.demon_casting"
        const val SNOWBALL_TRAVEL = "spotanim.raids_icedemon_iceball_travel"
        const val SNOWBALL_IMPACT = "spotanim.raids_icedemon_iceball_hit"
        const val BURST_TRAVEL = "spotanim.ice_burst_travel"
        const val BURST_IMPACT = "spotanim.ice_burst_impact"
        const val CHOP_SEQ = "seq.human_woodcutting_bronze_axe"
        const val LIGHT_SEQ = "seq.human_createfire"
        const val FUEL_SEQ = "seq.human_pickupfloor"
        const val CHOP_DELAY = 4
        const val LIGHT_DELAY = 3
        const val LIGHT_MIN = 8
        const val LIGHT_MAX = 78
        const val KINDLING_LEVEL_STEP = 12
        const val MAX_KINDLING = 28
        const val SAPLING_RESPAWN = 25
        const val RESIST_PERCENT = 33
        const val FIRE_PERCENT = 250
    }
}

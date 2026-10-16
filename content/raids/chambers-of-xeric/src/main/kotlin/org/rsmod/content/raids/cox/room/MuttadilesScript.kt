package org.rsmod.content.raids.cox.room

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType as EngineHitType
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.ScriptContext

class MuttadilesScript
@Inject
constructor(
    deps: BossDeps,
    private val points: CoxDamagePoints,
    private val services: CoxRoomServices,
    private val raids: CoxRaids,
) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onHit = { points.award(this) })
        BossCombat.register(this, smallSpec, deps, onHit = { points.award(this) })
        deps.extensionRegistry.register(STOMP) { ctx -> stomp(ctx.npc) }
        onOpNpc1(MuttadilesRoom.TREE) { chopTree(it.npc) }
    }

    private fun stomp(mutt: Npc) {
        val size = mutt.type.size
        for (player in services.players) {
            if (player.coords.level != mutt.coords.level) continue
            val dx = player.coords.x - mutt.coords.x
            val dz = player.coords.z - mutt.coords.z
            if (dx !in -1..size || dz !in -1..size) continue
            services.strike(mutt, player, EngineHitType.Melee, meleeType = MeleeAttackType.Crush, scale = STOMP_SCALE)
        }
    }

    private suspend fun ProtectedAccess.chopTree(tree: Npc) {
        val room = raids.containing(player)?.controllerOf(tree) as? MuttadilesRoom ?: return
        if (!hasAxe()) {
            mes("You need an axe to cut the meat tree.")
            return
        }
        while (tree.isSlotAssigned) {
            anim(CHOP_SEQ)
            delay(CHOP_DELAY)
            if (!tree.isSlotAssigned) break
            val level = player.woodcuttingLvl
            val chance = CHOP_BASE_CHANCE + (level - 1) * CHOP_LEVEL_CHANCE / CHOP_LEVEL_SPAN
            if (random.of(1, 100) > chance) continue
            val damage = (level / 2 - 2 + random.of(-CHOP_SPREAD, CHOP_SPREAD)).coerceAtLeast(1)
            if (room.chop(damage)) break
        }
        resetAnim()
    }

    private fun ProtectedAccess.hasAxe(): Boolean {
        val worn = player.righthand?.let { getInvObj(it) }
        if (worn != null && worn.isUsableAxe()) return true
        return inv.any { it != null && getInvObj(it).isUsableAxe() }
    }

    private fun ItemServerType.isUsableAxe(): Boolean = isContentType("content.woodcutting_axe")

    override val spec: BossSpec =
        boss(MuttadilesRoom.BIG, MuttadilesRoom.SUBMERGED) {
            stats(attackRate = 4)
            val bite = ability("bite") { melee("seq.dohgadyle_bite", MeleeAttackType.Crush) }
            val ranged =
                ability("ranged") { missile("seq.dohgadyle_bark", RANGED_TRAVEL, HitType.Ranged) }
            val magic =
                ability("magic") {
                    missile("seq.dohgadyle_lazor", MAGIC_TRAVEL, HitType.Magic, impact = MAGIC_IMPACT)
                }
            val stomp =
                ability("stomp") {
                    anim("seq.dohgadyle_slam")
                    include(external(STOMP))
                }
            phase("fight") {
                weightedSelectorRandom {
                    +random(bite, weight = 3, requires = Condition.WithinMeleeRange)
                    +random(stomp, weight = 1, requires = Condition.WithinMeleeRange, cooldown = 8)
                    +random(ranged, weight = 2)
                    +random(magic, weight = 2)
                }
            }
        }

    private val smallSpec: BossSpec =
        boss(MuttadilesRoom.SMALL) {
            stats(attackRate = 4)
            val bite = ability("bite") { melee("seq.dohgadyle_bite", MeleeAttackType.Crush) }
            val ranged =
                ability("ranged") { missile("seq.dohgadyle_bark", RANGED_TRAVEL, HitType.Ranged) }
            phase("fight") {
                weightedSelectorRandom {
                    +random(bite, weight = 3, requires = Condition.WithinMeleeRange)
                    +random(ranged, weight = 1)
                }
            }
        }

    private companion object {
        const val STOMP = "cox.muttadile_stomp"
        const val RANGED_TRAVEL = "spotanim.lizardman_spit"
        const val MAGIC_TRAVEL = "spotanim.fireblast_travel"
        const val MAGIC_IMPACT = "spotanim.fireblast_impact"
        const val STOMP_SCALE = 1.5
        const val CHOP_SEQ = "seq.human_woodcutting_bronze_axe"
        const val CHOP_DELAY = 4
        const val CHOP_BASE_CHANCE = 20
        const val CHOP_LEVEL_CHANCE = 58
        const val CHOP_LEVEL_SPAN = 98
        const val CHOP_SPREAD = 5
    }
}

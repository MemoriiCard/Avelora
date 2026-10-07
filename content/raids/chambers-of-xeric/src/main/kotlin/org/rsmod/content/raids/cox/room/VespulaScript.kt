package org.rsmod.content.raids.cox.room

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType as EngineHitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.ScriptContext

class VespulaScript
@Inject
constructor(
    deps: BossDeps,
    private val points: CoxDamagePoints,
    private val raids: CoxRaids,
) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                if (hit.type != EngineHitType.Melee) return@register
                val room = raids.at(npc.coords)?.controllerOf(npc) as? VespulaRoom ?: return@register
                val halberd = hit.righthandType()?.name?.lowercase()?.contains("halberd") == true
                if (room.isFlying(npc) && !halberd) hit.damage = 0
            },
            onHit = { points.award(this) },
        )
        onNpcHit(npcType(VespulaRoom.PORTAL)) {
            points.award(this, bonusPerDamage = VespulaRoom.PORTAL_BONUS_POINTS)
            (raids.at(npc.coords)?.controllerOf(npc) as? VespulaRoom)?.onPortalHit()
        }
        onNpcHit(npcType(VespulaRoom.SOLDIER)) { points.award(this) }
        for (grub in VespulaRoom.GRUB_TYPES) {
            onOpNpcU(grub) { if (it.objType.internalName == VespulaRoom.BLOSSOM) feedGrub(it.npc) }
        }
        onOpLoc1(VespulaRoom.HERB) { pickBlossom(it.loc) }
    }

    private fun ProtectedAccess.feedGrub(grubNpc: Npc) {
        val room = raids.containing(player)?.controllerOf(grubNpc) as? VespulaRoom ?: return
        val grub = room.grubOf(grubNpc) ?: return
        if (!grub.alive) {
            mes("It's too late to help this grub.")
            return
        }
        if (invDel(inv, VespulaRoom.BLOSSOM).failure) return
        anim(FEED_SEQ)
        grub.feed(VespulaRoom.BLOSSOM_HEAL)
    }

    private fun ProtectedAccess.pickBlossom(loc: BoundLocInfo) {
        val room = raids.containing(player)?.let { raid -> raid.roomAt(loc.coords)?.let(raid::controllerOf) }
        if (room !is VespulaRoom) return
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        anim(PICK_SEQ)
        invAdd(inv, VespulaRoom.BLOSSOM)
        room.pickBlossom(loc)
    }

    private fun npcType(name: String) =
        ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)) ?: error("Missing $name")

    override val spec: BossSpec =
        boss(*VespulaRoom.VESPULA_TYPES.toTypedArray()) {
            stats(attackRate = 5)
            val sting =
                ability("sting") {
                    anim("seq.vespula_attack_ranged_flying")
                    projectile(
                        spotanim = "spotanim.raids_vespula_poison",
                        travel = "projanim.arrow",
                        hit =
                            Effect.Hit(
                                damage = scaledHit(),
                                type = HitType.Ranged,
                                onHit = Effect.Poison(POISON),
                            ),
                    )
                }
            val jab =
                ability("jab") { melee("seq.vespula_attack_melee_flying", MeleeAttackType.Stab) }
            phase("fight") {
                weightedSelectorRandom {
                    +random(sting, weight = 3)
                    +random(jab, weight = 1, requires = Condition.WithinMeleeRange)
                }
            }
        }

    private companion object {
        const val POISON = 20
        const val FEED_SEQ = "seq.human_pickupfloor"
        const val PICK_SEQ = "seq.human_pickupfloor"
    }
}

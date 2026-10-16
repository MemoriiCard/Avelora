package org.rsmod.content.raids.cox.room

import org.rsmod.api.bosses.dsl.AbilityBuilder
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.combat.commons.types.MeleeAttackType

/** A hit that rolls the npc's accuracy, then damage up to its (party-scaled) max hit. */
internal fun scaledHit(meleeType: MeleeAttackType? = null, scale: Double = 1.0): DamageExpr =
    DamageExpr.Accuracy(
        on = DamageExpr.NpcMaxHit(meleeType, scale),
        meleeAttackType = meleeType,
    )

internal fun AbilityBuilder.melee(seq: String, meleeType: MeleeAttackType) {
    anim(seq)
    hit {
        damage(scaledHit(meleeType))
        type(HitType.Melee)
    }
}

internal fun AbilityBuilder.missile(
    seq: String,
    travel: String,
    style: HitType,
    impact: String? = null,
    penetration: Int = 0,
) {
    anim(seq)
    projectile(
        spotanim = travel,
        travel = if (style == HitType.Magic) "projanim.magic_spell" else "projanim.arrow",
        hit =
            Effect.Hit(
                damage = scaledHit(),
                type = style,
                spotanim = impact,
                penetration = penetration,
            ),
    )
}

package org.rsmod.content.bosses.smokedevil

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.hat
import org.rsmod.game.entity.Player

internal object SmokeProtection {
    private val MASKS = setOf("obj.slayer_facemask", "obj.gasmask")
    private val HELM_PREFIXES = listOf("obj.slayer_helm", "obj.sw_slayer_helm")

    fun isProtected(player: Player): Boolean {
        val hat = player.hat ?: return false
        val name = runCatching { RSCM.getReverseMapping(RSCMType.OBJ, hat.id) }.getOrNull() ?: return false
        return name in MASKS || HELM_PREFIXES.any(name::startsWith)
    }
}

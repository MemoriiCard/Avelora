package org.rsmod.content.bosses.chaoselemental

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.player.worn.WornUnequipOp
import org.rsmod.api.player.worn.WornUnequipResult
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class ChaosElemental
@Inject
constructor(deps: BossDeps, private val unequipOp: WornUnequipOp) : BossPluginScript(deps) {
    override val spec = chaosElementalSpec()

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)
        deps.extensionRegistry.register(CONFUSION_HANDLER) { _, _, target, _ -> confuse(target) }
        deps.extensionRegistry.register(MADNESS_HANDLER) { _, _, target, _ -> madden(target) }
    }

    private fun confuse(player: Player) {
        val dest = confusionTile(player.coords) ?: return
        PathingEntityCommon.telejump(player, deps.collision, dest)
    }

    private fun confusionTile(from: CoordGrid): CoordGrid? {
        repeat(CONFUSION_ATTEMPTS) {
            val dx = deps.random.of(-CONFUSION_RADIUS, CONFUSION_RADIUS)
            val dz = deps.random.of(-CONFUSION_RADIUS, CONFUSION_RADIUS)
            val tile = from.translate(dx, dz)
            if (tile != from && !deps.collision.isWalkBlocked(tile)) return tile
        }
        return null
    }

    private fun madden(player: Player) {
        val others =
            player.worn.indices.filter { it != WEAPON_SLOT && player.worn[it] != null }.shuffled()
        val slots = (listOf(WEAPON_SLOT).filter { player.worn[it] != null } + others)
        for (slot in slots.take(MADNESS_MAX_ITEMS)) {
            val result = unequipOp.unequip(player, slot, player.worn, player.inv)
            if (result !is WornUnequipResult.Success) break
        }
    }

    private companion object {
        const val CONFUSION_RADIUS = 5
        const val CONFUSION_ATTEMPTS = 20
        const val MADNESS_MAX_ITEMS = 4
        val WEAPON_SLOT = Wearpos.RightHand.slot
    }
}

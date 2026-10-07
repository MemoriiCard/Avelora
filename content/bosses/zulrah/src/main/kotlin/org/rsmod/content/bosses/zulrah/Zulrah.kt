package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Zulrah
@Inject
internal constructor(
    private val deps: BossDeps,
    private val fights: ZulrahFights,
    private val npcDeath: NpcDeath,
    private val instances: InstanceManager,
) : PluginScript() {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            zulrahSpec(),
            deps,
            onModifyHit = {
                if (hit.damage > DAMAGE_CAP_THRESHOLD) {
                    hit.damage = deps.random.of(DAMAGE_CAP_MIN, DAMAGE_CAP_MAX)
                }
            },
        )
        BossCombat.register(this, meleeSnakelingSpec(), deps)
        BossCombat.register(this, magicSnakelingSpec(), deps)

        deps.extensionRegistry.register(ZULRAH_NEXT_ACTION) { _, npc, target, _ ->
            fights.nextAction(npc, target)
        }
        deps.extensionRegistry.register(ZULRAH_VENOM) { _, _, target, _ -> PlayerVenom.tryVenom(target) }

        for (form in ZulrahForm.entries) {
            val type = ServerCacheManager.getNpc(form.npc.asRSCM(RSCMType.NPC)) ?: continue
            onNpcQueue(type, "queue.death") { zulrahDeath(this) }
        }
        for (snakeling in SNAKELINGS) {
            val type = ServerCacheManager.getNpc(snakeling.asRSCM(RSCMType.NPC)) ?: continue
            onNpcQueue(type, "queue.death") { snakelingDeath(this) }
        }
    }

    private suspend fun zulrahDeath(access: StandardNpcAccess) {
        val npc = access.npc
        val fight = fights.end(npc)
        access.noneMode()
        access.hideAllOps()
        access.anim(DEATH_SEQ)
        access.delay(DEATH_TICKS)
        if (fight != null) {
            npcDeath.spawnDrops(access, fight.player.coords)
            instances.resolveCoord(fight.session, ZulrahShrine.EXIT_SCROLL)?.let {
                deps.locRepo.add(it, EXIT_LOC, Int.MAX_VALUE, LocAngle.North, LocShape.CentrepieceStraight)
            }
        }
        deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    private suspend fun snakelingDeath(access: StandardNpcAccess) {
        access.noneMode()
        access.hideAllOps()
        access.anim(SNAKELING_DEATH_SEQ)
        access.delay(SNAKELING_DEATH_TICKS)
        deps.npcRepo.del(access.npc, Int.MAX_VALUE)
    }

    private companion object {
        const val DAMAGE_CAP_THRESHOLD = 50
        const val DAMAGE_CAP_MIN = 45
        const val DAMAGE_CAP_MAX = 50
        const val DEATH_TICKS = 4
        const val SNAKELING_DEATH_TICKS = 2
        const val DEATH_SEQ = "seq.snakeboss_death"
        const val SNAKELING_DEATH_SEQ = "seq.snakeboss_pet_death"
        const val EXIT_LOC = "loc.snakeboss_exit"

        val SNAKELINGS = listOf("npc.snakeboss_minion_melee", "npc.snakeboss_minion_magic")
    }
}

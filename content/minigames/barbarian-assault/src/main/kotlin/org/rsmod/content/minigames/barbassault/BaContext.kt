package org.rsmod.content.minigames.barbassault

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.modifier.StandardPlayerHitModifier
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.game.MapClock
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
internal class BaContext
@Inject
constructor(
    val players: PlayerList,
    val clock: MapClock,
    val collision: CollisionFlagMap,
    val npcs: NpcRepository,
    val objs: ObjRepository,
    val interactions: AiPlayerInteractions,
    val regions: RegionRepository,
    val registry: RegionRegistry,
    val random: GameRandom,
    val playerHits: StandardPlayerHitModifier,
    val npcHits: StandardNpcHitModifier,
    val worldQueues: WorldQueueList,
)

package org.rsmod.content.raids.cox.reward

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.obj.Obj

@Singleton
class CoxRewardDelivery @Inject constructor(private val objRepo: ObjRepository) {
    /** Adds [item] to [inv], dropping it at the player's feet when it doesn't fit. */
    fun give(player: Player, item: CoxItem, inv: Inventory = player.inv) {
        val base = ServerCacheManager.getItem(item.obj.asRSCM(RSCMType.OBJ)) ?: return
        val noted = item.noted && inv === player.inv && base.certlink > 0
        val added = player.invAdd(inv, item.obj, item.count, cert = noted).success
        if (added) return
        val dropped = if (noted) ServerCacheManager.getItem(base.certlink) ?: base else base
        val duration = player.lootDropDuration ?: constants.lootdrop_duration
        objRepo.add(Obj.fromOwner(player, player.coords, dropped, item.count), duration)
    }

    fun displayName(item: CoxItem): String =
        ServerCacheManager.getItem(item.obj.asRSCM(RSCMType.OBJ))?.name ?: item.obj

    fun owns(player: Player, obj: String): Boolean =
        player.inv.count(obj) > 0 || player.invMap.getOrPut("inv.bank").count(obj) > 0
}

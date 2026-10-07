package org.rsmod.api.dig

import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.map.CoordGrid

@Singleton
public class DigSites {
    private val sites = mutableListOf<DigSite>()

    public fun register(tiles: Collection<CoordGrid>, action: ProtectedAccess.() -> Unit) {
        sites += DigSite(tiles.toSet(), action)
    }

    public fun find(coords: CoordGrid): (ProtectedAccess.() -> Unit)? =
        sites.firstOrNull { coords in it.tiles }?.action

    private class DigSite(val tiles: Set<CoordGrid>, val action: ProtectedAccess.() -> Unit)
}

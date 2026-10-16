package org.rsmod.api.death

/** Marks a death as safe (e.g. the Fight Caves): the player keeps every item and nothing drops. */
public interface PlayerSafeDeathHook {
    public fun isSafeDeath(context: PlayerDeathContext): Boolean
}

package org.rsmod.content.bosses.zulrah

import org.rsmod.content.bosses.zulrah.ZulrahAction.Clouds
import org.rsmod.content.bosses.zulrah.ZulrahAction.Magic
import org.rsmod.content.bosses.zulrah.ZulrahAction.Melee
import org.rsmod.content.bosses.zulrah.ZulrahAction.Ranged
import org.rsmod.content.bosses.zulrah.ZulrahAction.Snakeling
import org.rsmod.content.bosses.zulrah.ZulrahAction.Tanzanite
import org.rsmod.content.bosses.zulrah.ZulrahForm.Magma
import org.rsmod.content.bosses.zulrah.ZulrahForm.Serpentine
import org.rsmod.content.bosses.zulrah.ZulrahPosition.East
import org.rsmod.content.bosses.zulrah.ZulrahPosition.Middle
import org.rsmod.content.bosses.zulrah.ZulrahPosition.South
import org.rsmod.content.bosses.zulrah.ZulrahPosition.West

internal enum class ZulrahForm(val npc: String) {
    Serpentine("npc.snakeboss_boss_ranged"),
    Magma("npc.snakeboss_boss_melee"),
    Tanzanite("npc.snakeboss_boss_magic"),
}

internal enum class ZulrahPosition {
    Middle,
    South,
    East,
    West,
}

internal enum class ZulrahAction {
    Ranged,
    Magic,
    Tanzanite,
    Melee,
    Clouds,
    Snakeling,
}

internal data class ZulrahPhase(
    val form: ZulrahForm,
    val position: ZulrahPosition,
    val actions: List<ZulrahAction>,
)

/**
 * The four rotations from the OSRS Wiki's Zulrah strategy guide. Each list starts at phase 2: the
 * fight opens on [OPENING], and every rotation ends on the middle green phase, which doubles as
 * phase 1 of the next randomly chosen rotation.
 */
internal object ZulrahRotations {
    val OPENING = ZulrahPhase(Serpentine, Middle, times(4, Clouds))

    val ROTATIONS: List<List<ZulrahPhase>> =
        listOf(
            listOf(
                phase(Magma, Middle, times(2, Melee)),
                phase(ZulrahForm.Tanzanite, Middle, times(4, Tanzanite)),
                phase(Serpentine, South, times(5, Ranged) + times(2, Snakeling) + times(2, Clouds) + times(2, Snakeling)),
                phase(Magma, Middle, times(2, Melee)),
                phase(ZulrahForm.Tanzanite, West, times(5, Tanzanite)),
                phase(Serpentine, South, times(3, Clouds) + times(4, Snakeling)),
                phase(ZulrahForm.Tanzanite, South, times(5, Tanzanite) + alternate(5, Snakeling, Clouds)),
                phase(Serpentine, West, alternate(10, Ranged, Magic) + times(4, Clouds)),
                phase(Magma, Middle, times(2, Melee)),
                phase(Serpentine, Middle, times(5, Ranged) + times(4, Clouds)),
            ),
            listOf(
                phase(Magma, Middle, times(2, Melee)),
                phase(ZulrahForm.Tanzanite, Middle, times(4, Tanzanite)),
                phase(Serpentine, West, times(3, Clouds) + times(4, Snakeling)),
                phase(ZulrahForm.Tanzanite, South, times(5, Tanzanite) + times(2, Snakeling) + times(2, Clouds) + times(2, Snakeling)),
                phase(Magma, Middle, times(2, Melee)),
                phase(Serpentine, East, times(5, Ranged)),
                phase(ZulrahForm.Tanzanite, South, times(5, Tanzanite) + alternate(5, Snakeling, Clouds)),
                phase(Serpentine, West, alternate(10, Ranged, Magic) + times(4, Clouds)),
                phase(Magma, Middle, times(2, Melee)),
                phase(Serpentine, Middle, times(5, Ranged) + times(4, Clouds)),
            ),
            listOf(
                phase(Serpentine, East, times(5, Ranged) + times(3, Snakeling)),
                phase(Magma, Middle, alternate(6, Clouds, Snakeling) + times(2, Melee)),
                phase(ZulrahForm.Tanzanite, West, times(5, Tanzanite)),
                phase(Serpentine, South, times(5, Ranged)),
                phase(ZulrahForm.Tanzanite, East, times(5, Tanzanite)),
                phase(Serpentine, Middle, times(3, Clouds) + times(3, Snakeling)),
                phase(Serpentine, West, times(5, Ranged)),
                phase(ZulrahForm.Tanzanite, Middle, times(5, Tanzanite) + times(2, Clouds) + times(3, Snakeling)),
                phase(Serpentine, East, alternate(10, Magic, Ranged)),
                phase(ZulrahForm.Tanzanite, Middle, times(4, Snakeling)),
                phase(Serpentine, Middle, times(5, Ranged) + times(4, Clouds)),
            ),
            listOf(
                phase(ZulrahForm.Tanzanite, East, times(4, Snakeling) + times(6, Tanzanite)),
                phase(Serpentine, South, times(4, Ranged) + times(2, Clouds)),
                phase(ZulrahForm.Tanzanite, West, times(4, Snakeling) + times(4, Tanzanite)),
                phase(Magma, Middle, times(2, Melee) + times(2, Clouds)),
                phase(Serpentine, East, times(4, Ranged)),
                phase(Serpentine, South, times(6, Snakeling) + times(3, Clouds)),
                phase(ZulrahForm.Tanzanite, West, times(5, Tanzanite) + times(4, Snakeling)),
                phase(Serpentine, Middle, times(4, Ranged)),
                phase(ZulrahForm.Tanzanite, Middle, times(4, Tanzanite) + times(3, Clouds)),
                phase(Serpentine, East, alternate(8, Magic, Ranged)),
                phase(ZulrahForm.Tanzanite, Middle, times(4, Snakeling)),
                phase(Serpentine, Middle, times(5, Ranged) + times(4, Clouds)),
            ),
        )

    private fun phase(form: ZulrahForm, position: ZulrahPosition, actions: List<ZulrahAction>) =
        ZulrahPhase(form, position, actions)

    private fun times(count: Int, action: ZulrahAction): List<ZulrahAction> = List(count) { action }

    private fun alternate(count: Int, first: ZulrahAction, second: ZulrahAction): List<ZulrahAction> =
        List(count) { if (it % 2 == 0) first else second }
}

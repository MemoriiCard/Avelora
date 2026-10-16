package org.rsmod.content.bosses.giantmole

import jakarta.inject.Inject
import org.rsmod.api.player.cinematic.Cinematic
import org.rsmod.api.player.cinematic.MinimapState
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenFullOverlay
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.random.GameRandom
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

private var Player.darkness by intVarBit("varbit.darkness_level")

class MoleHoleDarkness
@Inject
constructor(
    private val eventBus: EventBus,
    private val random: GameRandom,
    private val hitModifier: PlayerHitModifier,
) {
    fun hasLight(player: Player): Boolean =
        LIGHT_SOURCES.any { it in player.inv || it in player.worn }

    fun update(player: Player) {
        if (hasLight(player)) {
            brighten(player)
        } else {
            darken(player)
        }
    }

    fun tick(player: Player) {
        if (!MoleHole.contains(player.coords) || hasLight(player)) {
            brighten(player)
            return
        }
        player.mes("You are being bitten by insects in the dark!")
        player.queueHit(1, HitType.Typeless, random.of(INSECT_DAMAGE), hitModifier)
    }

    fun brighten(player: Player) {
        player.clearSoftTimer(TIMER)
        if (player.darkness == 0) return
        player.darkness = 0
        Cinematic.setMinimapState(player, MinimapState.Normal)
        player.ifCloseSub(OVERLAY, eventBus)
    }

    private fun darken(player: Player) {
        if (player.darkness != 0) return
        player.darkness = DARK
        Cinematic.setMinimapState(player, MinimapState.MinimapHidden)
        player.ifOpenFullOverlay(OVERLAY, eventBus)
        player.softTimer(TIMER, BITE_INTERVAL)
    }

    companion object {
        const val TIMER = "timer.mole_hole_darkness"
        private const val OVERLAY = "interface.darkness_dark"
        private const val DARK = 3
        private const val BITE_INTERVAL = 16
        private val INSECT_DAMAGE = 1..3

        val LIGHT_SOURCES =
            listOf(
                "obj.lit_candle",
                "obj.lit_black_candle",
                "obj.torch_lit",
                "obj.candle_lantern_lit",
                "obj.candle_lantern_black_lit",
                "obj.oil_lamp_lit",
                "obj.oil_lantern_lit",
                "obj.bullseye_lantern_lit",
                "obj.cave_goblin_mining_helmet_lit",
            )

        val OPEN_FLAMES =
            mapOf(
                "obj.lit_candle" to "obj.unlit_candle",
                "obj.lit_black_candle" to "obj.unlit_black_candle",
                "obj.torch_lit" to "obj.torch_unlit",
                "obj.oil_lamp_lit" to "obj.oil_lamp_unlit",
            )
    }
}

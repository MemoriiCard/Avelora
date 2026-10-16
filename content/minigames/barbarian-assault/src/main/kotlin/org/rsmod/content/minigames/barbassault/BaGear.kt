package org.rsmod.content.minigames.barbassault

internal enum class BaGear(
    val label: String,
    val obj: String,
    val cost: Int,
    val needsQueen: Boolean,
) {
    FighterHat("Penance fighter hat (220)", "obj.barbassault_penance_fighter_hat", 220, true),
    RangerHat("Penance ranger hat (220)", "obj.barbassault_penance_ranger_hat", 220, true),
    HealerHat("Penance healer hat (220)", "obj.barbassault_penance_healer_hat", 220, true),
    RunnerHat("Penance runner hat (220)", "obj.barbassault_penance_runner_hat", 220, true),
    Torso("Fighter torso (300)", "obj.barbassault_penance_fighter_torso", 300, true),
    Skirt("Penance skirt (300)", "obj.barbassault_penance_ranger_legs", 300, true),
    Gloves("Penance gloves (120)", "obj.barbassault_penance_gloves", 120, false),
    Boots("Runner boots (80)", "obj.barbassault_penance_runner_boots", 80, false),
    ;

    fun blocked(points: Int, queenKills: Int): String? =
        when {
            points < cost -> "You need $cost points in that role."
            needsQueen && queenKills < 1 -> "You need to have killed a Penance Queen first."
            else -> null
        }

    companion object {
        const val GRANITE_BODY = "obj.granite_body"
        const val GRANITE_BODY_COINS = 95_000
        const val GRANITE_BODY_QUEEN_KILLS = 7
        const val PET = "obj.penancepet"
        const val PET_ONE_IN = 1000

        fun graniteBodyBlocked(coins: Int, queenKills: Int): String? =
            when {
                queenKills < GRANITE_BODY_QUEEN_KILLS ->
                    "You need $GRANITE_BODY_QUEEN_KILLS Penance Queen kills."
                coins < GRANITE_BODY_COINS -> "You need $GRANITE_BODY_COINS coins."
                else -> null
            }
    }
}

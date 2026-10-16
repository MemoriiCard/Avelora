package org.rsmod.content.minigames.barbassault

internal class GambleItem(val obj: String, val min: Int, val max: Int, val noted: Boolean)

internal enum class GambleTier(val label: String, val cost: Int, val minWaves: Int, val needsQueen: Boolean) {
    Low("Low gamble (200 points)", 200, 3, false),
    Medium("Medium gamble (400 points)", 400, 6, false),
    High("High gamble (500 points)", 500, 0, true),
}

internal class GambleResult(val item: GambleItem, val quantity: Int, val rare: Boolean = false)

internal object BaGamble {
    const val RARE_ONE_IN = 16_000

    val LOW =
        listOf(
            GambleItem("obj.harralander", 15, 25, true),
            GambleItem("obj.toadflax", 15, 25, true),
            GambleItem("obj.irit_leaf", 15, 25, true),
            GambleItem("obj.harralander_seed", 5, 7, false),
            GambleItem("obj.toadflax_seed", 5, 7, false),
            GambleItem("obj.irit_seed", 5, 7, false),
            GambleItem("obj.strawberry_seed", 20, 20, false),
            GambleItem("obj.pineapple_tree_seed", 1, 2, false),
            GambleItem("obj.maple_seed", 1, 2, false),
            GambleItem("obj.rune_axe", 1, 1, false),
            GambleItem("obj.rune_med_helm", 1, 1, false),
            GambleItem("obj.rune_dagger_p++", 1, 1, false),
            GambleItem("obj.lava_battlestaff", 1, 1, false),
            GambleItem("obj.adamant_sq_shield", 1, 1, false),
            GambleItem("obj.iron_ore", 200, 200, true),
            GambleItem("obj.feather", 500, 750, false),
            GambleItem("obj.coal", 150, 150, true),
            GambleItem("obj.mithril_ore", 20, 20, true),
            GambleItem("obj.mithril_bar", 15, 15, true),
            GambleItem("obj.flax", 50, 75, true),
            GambleItem("obj.dragonhide_blue", 10, 20, true),
            GambleItem("obj.uncut_emerald", 25, 30, true),
            GambleItem("obj.maple_logs", 150, 200, true),
            GambleItem("obj.plank_oak", 60, 80, true),
            GambleItem("obj.mithril_arrow", 300, 400, false),
            GambleItem("obj.raw_lobster", 125, 150, true),
            GambleItem("obj.tuna", 90, 110, true),
            GambleItem("obj.cosmicrune", 150, 200, false),
            GambleItem("obj.chaosrune", 150, 200, false),
            GambleItem("obj.limpwurt_root", 10, 15, true),
            GambleItem("obj.unicorn_horn", 10, 15, true),
            GambleItem("obj.coins", 15000, 20000, false),
        )

    val MEDIUM =
        listOf(
            GambleItem("obj.avantoe", 25, 35, true),
            GambleItem("obj.kwuarm", 25, 35, true),
            GambleItem("obj.dwarf_weed", 25, 35, true),
            GambleItem("obj.avantoe_seed", 10, 14, false),
            GambleItem("obj.kwuarm_seed", 10, 14, false),
            GambleItem("obj.dwarf_weed_seed", 10, 14, false),
            GambleItem("obj.watermelon_seed", 20, 20, false),
            GambleItem("obj.papaya_tree_seed", 1, 2, false),
            GambleItem("obj.yew_seed", 1, 2, false),
            GambleItem("obj.rune_battleaxe", 1, 1, false),
            GambleItem("obj.rune_sq_shield", 1, 1, false),
            GambleItem("obj.rune_full_helm", 1, 1, false),
            GambleItem("obj.mystic_lava_staff", 1, 1, false),
            GambleItem("obj.keyhalf2", 1, 1, false),
            GambleItem("obj.keyhalf1", 1, 1, false),
            GambleItem("obj.feather", 1200, 1500, false),
            GambleItem("obj.coal", 400, 400, true),
            GambleItem("obj.adamantite_ore", 40, 40, true),
            GambleItem("obj.adamantite_bar", 25, 25, true),
            GambleItem("obj.big_bones", 120, 120, true),
            GambleItem("obj.dragonhide_red", 20, 30, true),
            GambleItem("obj.uncut_ruby", 25, 30, true),
            GambleItem("obj.yew_logs", 150, 200, true),
            GambleItem("obj.plank_teak", 60, 80, true),
            GambleItem("obj.adamant_arrow", 400, 500, false),
            GambleItem("obj.naturerune", 250, 300, false),
            GambleItem("obj.deathrune", 250, 300, false),
            GambleItem("obj.raw_swordfish", 180, 210, true),
            GambleItem("obj.swordfish", 90, 110, true),
            GambleItem("obj.limpwurt_root", 20, 30, true),
            GambleItem("obj.unicorn_horn", 20, 30, true),
            GambleItem("obj.coins", 30000, 40000, false),
        )

    val HIGH =
        listOf(
            GambleItem("obj.ranarr_weed", 20, 30, true),
            GambleItem("obj.snapdragon", 20, 30, true),
            GambleItem("obj.torstol", 20, 30, true),
            GambleItem("obj.ranarr_seed", 3, 4, false),
            GambleItem("obj.snapdragon_seed", 3, 4, false),
            GambleItem("obj.torstol_seed", 3, 4, false),
            GambleItem("obj.watermelon_seed", 50, 50, false),
            GambleItem("obj.palm_tree_seed", 1, 2, false),
            GambleItem("obj.magic_tree_seed", 1, 1, false),
            GambleItem("obj.viking_helmet_range", 1, 1, false),
            GambleItem("obj.viking_helmet_crush", 1, 1, false),
            GambleItem("obj.viking_helmet_slash", 1, 1, false),
            GambleItem("obj.viking_helmet_magic", 1, 1, false),
            GambleItem("obj.granite_shield", 1, 1, false),
            GambleItem("obj.granite_helm", 1, 1, false),
            GambleItem("obj.rune_kiteshield", 1, 1, false),
            GambleItem("obj.coal", 750, 750, true),
            GambleItem("obj.runite_ore", 20, 20, true),
            GambleItem("obj.runite_bar", 15, 15, true),
            GambleItem("obj.dragon_bones", 50, 75, true),
            GambleItem("obj.dragonhide_black", 30, 50, true),
            GambleItem("obj.uncut_diamond", 25, 30, true),
            GambleItem("obj.magic_logs", 50, 100, true),
            GambleItem("obj.plank_mahogany", 60, 80, true),
            GambleItem("obj.rune_arrow", 400, 500, false),
            GambleItem("obj.raw_shark", 250, 300, true),
            GambleItem("obj.shark", 75, 125, true),
            GambleItem("obj.lawrune", 250, 300, false),
            GambleItem("obj.bloodrune", 250, 300, false),
            GambleItem("obj.limpwurt_root", 30, 50, true),
            GambleItem("obj.unicorn_horn", 30, 50, true),
            GambleItem("obj.coins", 50000, 75000, false),
        )

    val RARE =
        listOf(
            GambleItem("obj.dragon_med_helm", 1, 1, false),
            GambleItem("obj.dragon_chainbody", 1, 1, false),
        )

    fun table(tier: GambleTier): List<GambleItem> =
        when (tier) {
            GambleTier.Low -> LOW
            GambleTier.Medium -> MEDIUM
            GambleTier.High -> HIGH
        }

    fun available(tier: GambleTier, points: Int, wavesProgress: Int, queenKills: Int): String? =
        when {
            points < tier.cost -> "You need ${tier.cost} points in that role."
            wavesProgress < tier.minWaves -> "You need to have reached wave ${tier.minWaves} first."
            tier.needsQueen && queenKills < 1 -> "You need to have killed a Penance Queen first."
            else -> null
        }

    fun roll(tier: GambleTier, roll: (Int) -> Int): GambleResult {
        if (tier == GambleTier.High && roll(RARE_ONE_IN) < RARE.size) {
            val item = RARE[roll(RARE.size)]
            return GambleResult(item, 1, rare = true)
        }
        val table = table(tier)
        val item = table[roll(table.size)]
        val quantity = if (item.max > item.min) item.min + roll(item.max - item.min + 1) else item.min
        return GambleResult(item, quantity)
    }
}

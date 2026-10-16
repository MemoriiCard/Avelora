package org.rsmod.content.minigames.lms

data class LmsLootEntry(val obj: String, val weight: Int, val count: Int = 1)

object LmsLoot {
    val TABLE =
        listOf(
            LmsLootEntry("obj.br_shark", 20),
            LmsLootEntry("obj.br_swordfish", 15),
            LmsLootEntry("obj.br_monkfish", 15),
            LmsLootEntry("obj.br_bass", 10),
            LmsLootEntry("obj.br_prayer4", 10),
            LmsLootEntry("obj.br_energy4", 6),
            LmsLootEntry("obj.br_4dose2combat", 8),
            LmsLootEntry("obj.br_4doserangerspotion", 6),
            LmsLootEntry("obj.br_4dose2restore", 6),
            LmsLootEntry("obj.br_rune_scimitar", 8),
            LmsLootEntry("obj.br_dragon_scimitar", 5),
            LmsLootEntry("obj.br_abyssal_whip", 4),
            LmsLootEntry("obj.br_dragon_dagger", 4),
            LmsLootEntry("obj.br_magic_bow", 4),
            LmsLootEntry("obj.br_rune_battleaxe", 3),
            LmsLootEntry("obj.br_obsidian_sword", 3),
            LmsLootEntry("obj.br_granite_maul", 3),
            LmsLootEntry("obj.br_dragon_claws", 2),
            LmsLootEntry("obj.br_ags", 1),
            LmsLootEntry("obj.br_rune_platebody", 5),
            LmsLootEntry("obj.br_rune_platelegs", 5),
            LmsLootEntry("obj.br_dragon_helm", 3),
            LmsLootEntry("obj.br_mystic_body", 4),
            LmsLootEntry("obj.br_mystic_legs", 4),
            LmsLootEntry("obj.br_blackdhide_body", 4),
            LmsLootEntry("obj.br_blackdhide_chaps", 4),
            LmsLootEntry("obj.br_adamant_arrow_pack", 4, 25),
            LmsLootEntry("obj.br_rune_arrow_pack", 3, 25),
            LmsLootEntry("obj.br_elemental_rune_pack", 3, 50),
        )

    private val TOTAL = TABLE.sumOf { it.weight }

    fun roll(maxExclusive: (Int) -> Int): LmsLootEntry {
        var pick = maxExclusive(TOTAL)
        for (entry in TABLE) {
            pick -= entry.weight
            if (pick < 0) return entry
        }
        return TABLE.last()
    }
}

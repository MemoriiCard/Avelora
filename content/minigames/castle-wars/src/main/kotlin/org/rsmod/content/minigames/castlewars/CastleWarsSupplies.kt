package org.rsmod.content.minigames.castlewars

internal class SupplyTable(val loc: String, val obj: String)

internal object CastleWarsSupplies {
    val tables =
        listOf(
            SupplyTable("loc.castlewars_table_toolbox", "obj.castlewars_toolkit"),
            SupplyTable("loc.castlewars_table_rocks", "obj.castlewars_catapult_rock"),
            SupplyTable("loc.castlewars_table_barricades", "obj.castlewars_barricade"),
            SupplyTable("loc.castlewars_table_rope", "obj.castlewars_climbing_rope"),
            SupplyTable("loc.castlewars_table_potion", "obj.castlewars_explosives_potion"),
            SupplyTable("loc.castlewars_table_pickaxes", "obj.bronze_pickaxe"),
            SupplyTable("loc.castlewars_table_buckets", "obj.bucket_empty"),
            SupplyTable("loc.castlewars_table_bandages_saradomin", "obj.castlewars_bandages"),
            SupplyTable("loc.castlewars_table_bandages_zamorak", "obj.castlewars_bandages"),
        )
}

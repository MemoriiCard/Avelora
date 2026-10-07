package org.rsmod.content.raids.cox

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Test

class CoxGamevalTest {
    @Test
    fun `every symbol the raid uses resolves`() {
        val cache = ServerCacheManager.init(240)
        try {
            for ((type, names) in SYMBOLS) {
                for (name in names) name.asRSCM(type)
            }
        } finally {
            cache.close()
        }
    }

    private companion object {
        val SYMBOLS =
            mapOf(
                RSCMType.INTERFACE to
                    listOf(
                        "interface.raids_lobby_partylist",
                        "interface.raids_lobby_partydetails",
                        "interface.raids_sidepanel",
                        "interface.raids_overlay",
                        "interface.side_journal",
                    ),
                RSCMType.COMPONENT to
                    listOf(
                        "component.raids_lobby_partylist:contents",
                        "component.raids_lobby_partylist:list",
                        "component.raids_lobby_partydetails:contents",
                        "component.raids_sidepanel:start",
                        "component.raids_sidepanel:refresh",
                        "component.toplevel_osrs_stretch:side2",
                        "component.toplevel_osrs_stretch:overlay_hud",
                    ),
                RSCMType.LOC to
                    listOf(
                        "loc.raids_party_recruitment",
                        "loc.raids_entrance_steps",
                        "loc.raids_exit_steps",
                        "loc.raids_exit_steps_reload",
                        "loc.raids_bank_chest_lobby_working",
                        "loc.raids_doorway",
                        "loc.raids_descentto2",
                        "loc.raids_descentto1",
                        "loc.raids_bossentrance",
                        "loc.raids_ascentto3",
                        "loc.raids_ascentto2",
                        "loc.raids_bossexit",
                        "loc.raids_olm_barrier",
                    ),
                RSCMType.CLIENTSCRIPT to
                    listOf(
                        "clientscript.[clientscript,raids_partylist_addline]",
                        "clientscript.[clientscript,raids_partydetails_addline]",
                        "clientscript.[clientscript,script1524]",
                        "clientscript.[clientscript,raids_sidepanel_initlines]",
                        "clientscript.[clientscript,raids_sidepanel_addline]",
                    ),
                RSCMType.VARBIT to
                    listOf(
                        "varbit.raids_partymember_id",
                        "varbit.raids_daily_adverts",
                        "varbit.raids_client_isleader",
                        "varbit.raids_client_partysize",
                        "varbit.raids_client_partysize_scaled",
                        "varbit.raids_client_highestcombat",
                        "varbit.raids_client_progress",
                        "varbit.raids_client_indungeon",
                        "varbit.raids_client_partyscore",
                        "varbit.raids_died",
                        "varbit.raids_lobby_partysize",
                        "varbit.raids_lobby_mincombat",
                        "varbit.raids_lobby_minskilltotal",
                        "varbit.raids_scaling",
                        "varbit.raids_map_pool_selected",
                        "varbit.raids_challenge_mode",
                        "varbit.raids_timer",
                    ),
                RSCMType.VARP to
                    listOf(
                        "varp.raids_party_groupholder",
                        "varp.raids_playerscore",
                        "varp.total_completed_xericchambers",
                        "varp.total_completed_xericchambers_challenge",
                    ),
            )
    }
}

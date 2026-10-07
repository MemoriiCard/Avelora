package org.rsmod.content.raids.cox.party

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

internal var Player.raidsPartyHolder by intVarp("varp.raids_party_groupholder")
internal var Player.raidsPartyMemberId by intVarBit("varbit.raids_partymember_id")
internal var Player.raidsDailyAdverts by intVarBit("varbit.raids_daily_adverts")

internal var Player.raidsIsLeader by boolVarBit("varbit.raids_client_isleader")
internal var Player.raidsClientPartySize by intVarBit("varbit.raids_client_partysize")
internal var Player.raidsClientScaledSize by intVarBit("varbit.raids_client_partysize_scaled")
internal var Player.raidsClientHighestCombat by intVarBit("varbit.raids_client_highestcombat")
internal var Player.raidsClientProgress by intVarBit("varbit.raids_client_progress")
internal var Player.raidsClientInDungeon by boolVarBit("varbit.raids_client_indungeon")
internal var Player.raidsClientPartyScore by intVarBit("varbit.raids_client_partyscore")
internal var Player.raidsPlayerScore by intVarp("varp.raids_playerscore")
internal var Player.raidsDied by boolVarBit("varbit.raids_died")

internal var Player.raidsLobbyPartySize by intVarBit("varbit.raids_lobby_partysize")
internal var Player.raidsLobbyMinCombat by intVarBit("varbit.raids_lobby_mincombat")
internal var Player.raidsLobbyMinSkillTotal by intVarBit("varbit.raids_lobby_minskilltotal")
internal var Player.raidsScaling by intVarBit("varbit.raids_scaling")
internal var Player.raidsMapPool by intVarBit("varbit.raids_map_pool_selected")
internal var Player.raidsChallengeMode by boolVarBit("varbit.raids_challenge_mode")
internal var Player.raidsTimer by intVarBit("varbit.raids_timer")

internal var Player.coxViewingParty by intVarp("varp.cox_viewing_party")

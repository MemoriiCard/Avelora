package org.rsmod.content.quest.area.falador

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class DoricsQuestInteractionTest {
    @Test fun `accepting starts the quest and declining does not`() {
        val f = Fixture()
        f.talk(); f.finish(listOf(1, 2))
        assertEquals(0, f.script.quest.getQuestStage(f.player))
        f.talk(); f.finish(listOf(1, 1))
        assertEquals(10, f.script.quest.getQuestStage(f.player))
        assertEquals(10, f.player.vars["varp.doricquest"])
    }

    @Test fun `missing materials keep the quest open and nothing is taken`() {
        val f = Fixture(stage = 10)
        f.materials(iron = 1)
        f.talk(); f.finish()
        assertEquals(10, f.script.quest.getQuestStage(f.player))
        assertEquals(6, f.player.inv.count("obj.clay"))
        assertEquals(1, f.player.inv.count("obj.iron_ore"))
    }

    @Test fun `handing over every material completes the quest once`() {
        val f = Fixture(stage = 10)
        f.materials()
        f.talk(); f.finish()
        f.assertReward()
        assertEquals(0, f.player.inv.count("obj.clay"))
        f.talk(); f.finish()
        f.assertReward()
    }

    private class Fixture(stage: Int = 0) {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("dorics-quest-test")
        private var result: Result<Unit>? = null
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
            getNpcInteractions = { NpcInteractions(events) },
        )

        @OptIn(InternalApi::class)
        val player = Player().apply {
            this.client = this@Fixture.client
            uuid = 790L
            slotId = 1
            assignUid()
            coords = CoordGrid(3207, 3214, 0)
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

        val script = DoricsQuest()

        init {
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            VarPlayerIntMapSetter.set(player, "varbit.dorics_quest_progress", stage)
        }

        private fun access() = ProtectedAccess(player, coroutine, context)

        fun materials(clay: Int = 6, copper: Int = 4, iron: Int = 2) {
            var slot = 0
            repeat(clay) { player.inv[slot++] = InvObj("obj.clay", 1) }
            repeat(copper) { player.inv[slot++] = InvObj("obj.copper_ore", 1) }
            repeat(iron) { player.inv[slot++] = InvObj("obj.iron_ore", 1) }
        }

        fun talk() {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val npc = Npc("npc.doric", player.coords.translateZ(1))
            val block: suspend () -> Unit = { assertTrue(events.publish(access(), NpcEvents.Op1(npc))) }
            block.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
        }

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(150) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val parent = listOf("chat_left", "chat_right", "messagebox", "chatmenu")
                    .firstOrNull { player.ui.containsModal("interface.$it") }
                    ?: error("Unknown dialogue: ${output()}")
                val input = if (parent == "chatmenu") {
                    ResumePauseButtonInput("component.chatmenu:options", if (options.hasNext()) options.next() else 1)
                } else ResumePauseButtonInput("component.$parent:continue", -1)
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }

        fun assertReward() {
            assertEquals(100, script.quest.getQuestStage(player))
            assertEquals(100, player.vars["varp.doricquest"])
            assertEquals(1, player.vars["varp.qp"])
            assertEquals(1300, player.statMap.getXP("stat.mining"))
            assertEquals(180, player.inv.count("obj.coins"))
        }

        fun output() = client.messages.joinToString("\n")
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()
        override fun write(message: Any) { messages += message }
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            ServerCacheManager.init(241).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic @AfterAll fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}

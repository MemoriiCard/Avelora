package org.rsmod.content.quest.area.portsarim

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
import org.rsmod.api.dig.DigSites
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
import org.rsmod.api.script.onOpNpc1
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
class PiratesTreasureInteractionTest {
    @Test fun `frank starts the quest and swaps rum for the chest key`() {
        val f = Fixture()
        f.talk("npc.redbeard_frank"); f.finish(listOf(1))
        assertEquals(1, f.script.quest.getQuestStage(f.player))
        f.talk("npc.redbeard_frank"); f.finish()
        assertEquals(1, f.script.quest.getQuestStage(f.player))
        f.player.inv[0] = InvObj("obj.karamja_rum", 1)
        f.talk("npc.redbeard_frank"); f.finish()
        assertEquals(2, f.script.quest.getQuestStage(f.player))
        assertEquals(0, f.player.inv.count("obj.karamja_rum"))
        assertEquals(1, f.player.inv.count("obj.chest_key"))
    }

    @Test fun `frank replaces a lost chest key`() {
        val f = Fixture(stage = 2)
        f.talk("npc.redbeard_frank"); f.finish()
        assertEquals(1, f.player.inv.count("obj.chest_key"))
    }

    @Test fun `the rum is only handed over from the store room crate after it was shipped`() {
        val f = Fixture(stage = 1)
        assertFalse(f.searchCrate())
        VarPlayerIntMapSetter.set(f.player, "varbit.pirates_treasure_rum_shipped", 1)
        assertTrue(f.searchCrate())
        assertEquals(0, f.player.inv.count("obj.karamja_rum"))
        f.player.worn[0] = InvObj("obj.white_apron", 1)
        assertTrue(f.searchCrate())
        assertEquals(1, f.player.inv.count("obj.karamja_rum"))
    }

    @Test fun `the customs officer takes any rum on the way back`() {
        val f = Fixture(stage = 1)
        f.player.inv[0] = InvObj("obj.karamja_rum", 1)
        f.confiscate()
        assertEquals(0, f.player.inv.count("obj.karamja_rum"))
    }

    private class Fixture(stage: Int = 0) {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("pirates-treasure-test")
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

        val script = PiratesTreasure(DigSites())

        init {
            val scriptContext = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(script) { scriptContext.startup() }
            with(scriptContext) {
                onOpNpc1("npc.redbeard_frank") { startDialogue(it.npc) { with(PiratesTreasureHooks) { frank() } } }
            }
            VarPlayerIntMapSetter.set(player, "varbit.pirates_treasure_progress", stage)
        }

        private fun access() = ProtectedAccess(player, coroutine, context)

        fun searchCrate() = with(PiratesTreasureHooks) { access().searchCrate() }

        fun confiscate(): Boolean {
            var result = false
            val block: suspend () -> Unit = { result = with(PiratesTreasureHooks) { access().confiscateRum() } }
            block.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) {}
            })
            return result
        }

        fun talk(name: String) {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val npc = Npc(name, player.coords.translateZ(1))
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
                val parent = listOf("chat_left", "chat_right", "messagebox", "chatmenu", "objectbox")
                    .firstOrNull { player.ui.containsModal("interface.$it") }
                    ?: error("Unknown dialogue: ${output()}")
                val input = when (parent) {
                    "chatmenu" ->
                        ResumePauseButtonInput("component.chatmenu:options", if (options.hasNext()) options.next() else 1)
                    "objectbox" -> ResumePauseButtonInput("component.objectbox:universe", -1)
                    else -> ResumePauseButtonInput("component.$parent:continue", -1)
                }
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
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

package org.rsmod.content.quest.area.varrock

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
class RomeoAndJulietInteractionTest {
    @Test fun `declining leaves the quest unstarted and helping starts it`() {
        val f = Fixture()
        f.talk("npc.romeo"); f.finish(listOf(2))
        assertEquals(0, f.script.quest.getQuestStage(f.player))
        f.talk("npc.romeo"); f.finish(listOf(3))
        assertEquals(10, f.script.quest.getQuestStage(f.player))
        assertEquals(10, f.player.vars["varp.rjquest"])
    }

    @Test fun `juliet hands over her message and romeo takes it`() {
        val f = Fixture(stage = 10)
        f.talk("npc.juliet"); f.finish()
        assertEquals(20, f.script.quest.getQuestStage(f.player))
        assertEquals(1, f.player.inv.count("obj.julietmessage"))
        f.talk("npc.romeo"); f.finish()
        assertEquals(30, f.script.quest.getQuestStage(f.player))
        assertEquals(0, f.player.inv.count("obj.julietmessage"))
    }

    @Test fun `romeo without the message keeps the stage`() {
        val f = Fixture(stage = 20)
        f.talk("npc.romeo"); f.finish()
        assertEquals(20, f.script.quest.getQuestStage(f.player))
    }

    @Test fun `lawrence and the apothecary move the quest along`() {
        val f = Fixture(stage = 30)
        f.talk("npc.father_lawrence"); f.finish()
        assertEquals(40, f.script.quest.getQuestStage(f.player))
        f.talk("npc.apothecary"); f.finish()
        assertEquals(50, f.script.quest.getQuestStage(f.player))
    }

    @Test fun `the apothecary swaps berries for the potion only with berries`() {
        val f = Fixture(stage = 50)
        f.talk("npc.apothecary"); f.finish()
        assertEquals(50, f.script.quest.getQuestStage(f.player))
        f.player.inv[0] = InvObj("obj.cadavaberries", 1)
        f.talk("npc.apothecary"); f.finish()
        assertEquals(60, f.script.quest.getQuestStage(f.player))
        assertEquals(0, f.player.inv.count("obj.cadavaberries"))
        assertEquals(1, f.player.inv.count("obj.cadava"))
    }

    @Test fun `the full quest completes with five quest points`() {
        val f = Fixture(stage = 60)
        f.player.inv[0] = InvObj("obj.cadava", 1)
        f.talk("npc.juliet"); f.finish()
        assertEquals(70, f.script.quest.getQuestStage(f.player))
        assertEquals(0, f.player.inv.count("obj.cadava"))
        f.talk("npc.romeo"); f.finish()
        assertEquals(100, f.script.quest.getQuestStage(f.player))
        assertEquals(100, f.player.vars["varp.rjquest"])
        assertEquals(5, f.player.vars["varp.qp"])
    }

    private class Fixture(stage: Int = 0) {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("romeo-juliet-test")
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

        val script = RomeoAndJuliet()

        init {
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            VarPlayerIntMapSetter.set(player, "varbit.romeo_juliet_progress", stage)
        }

        private fun access() = ProtectedAccess(player, coroutine, context)

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

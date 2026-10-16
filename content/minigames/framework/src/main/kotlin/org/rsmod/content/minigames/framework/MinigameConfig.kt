package org.rsmod.content.minigames.framework

import com.fasterxml.jackson.dataformat.toml.TomlMapper

data class GameSettings(val target: Int, val botFreeAt: Int)

class MinigameConfig(private val overrides: Map<String, GameSettings> = emptyMap()) {
    fun settings(game: Minigame): GameSettings =
        overrides[game.key] ?: GameSettings(game.defaultTarget, game.defaultBotFreeAt)

    companion object {
        private const val RESOURCE = "/minigames.toml"

        fun parse(text: String): MinigameConfig {
            val root = TomlMapper().readValue(text, Map::class.java)
            val overrides = HashMap<String, GameSettings>()
            for (game in Minigame.entries) {
                val section = root[game.key] as? Map<*, *> ?: continue
                overrides[game.key] =
                    GameSettings(
                        target = (section["target"] as? Number)?.toInt() ?: game.defaultTarget,
                        botFreeAt = (section["bot_free_at"] as? Number)?.toInt() ?: game.defaultBotFreeAt,
                    )
            }
            return MinigameConfig(overrides)
        }

        fun load(): MinigameConfig {
            val text = MinigameConfig::class.java.getResource(RESOURCE)?.readText() ?: return MinigameConfig()
            return parse(text)
        }
    }
}

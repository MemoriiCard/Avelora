package org.rsmod.content.raids.toa.party

enum class ToaMode(val label: String, val minLevel: Int, val awardsUniques: Boolean) {
    Entry("Entry Mode", 0, false),
    Normal("Normal Mode", 150, true),
    Expert("Expert Mode", 300, true);

    companion object {
        fun of(raidLevel: Int): ToaMode = entries.last { raidLevel >= it.minLevel }
    }
}

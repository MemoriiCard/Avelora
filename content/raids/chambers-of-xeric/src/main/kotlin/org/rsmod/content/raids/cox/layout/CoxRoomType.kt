package org.rsmod.content.raids.cox.layout

enum class CoxRoomType(
    val category: CoxRoomCategory,
    val templateX: Int,
    val templateZ: Int,
    val templateLevel: Int,
    val hasVariants: Boolean = true,
) {
    Lobby(CoxRoomCategory.Start, 3264, 5184, 0),
    FloorStart(CoxRoomCategory.Start, 3264, 5696, 0),
    FloorStartLower(CoxRoomCategory.Start, 3264, 5728, 0),
    FloorEnd(CoxRoomCategory.End, 3264, 5152, 0, hasVariants = false),
    FloorEndMiddle(CoxRoomCategory.End, 3264, 5120, 0, hasVariants = false),
    OlmEntrance(CoxRoomCategory.End, 3296, 5152, 0, hasVariants = false),

    Scavengers(CoxRoomCategory.Scavenger, 3264, 5216, 0),
    ScavengersLarge(CoxRoomCategory.Scavenger, 3264, 5216, 1),
    FarmingFishing(CoxRoomCategory.Farming, 3264, 5440, 0),
    FarmingBats(CoxRoomCategory.Farming, 3264, 5440, 1),

    Tekton(CoxRoomCategory.Combat, 3264, 5280, 1),
    Vasa(CoxRoomCategory.Combat, 3264, 5280, 0),
    Guardians(CoxRoomCategory.Combat, 3264, 5248, 2),
    Mystics(CoxRoomCategory.Combat, 3264, 5248, 1),
    Shamans(CoxRoomCategory.Combat, 3264, 5248, 0),
    Muttadiles(CoxRoomCategory.Combat, 3264, 5312, 1),
    Vanguards(CoxRoomCategory.Combat, 3264, 5312, 0),
    Vespula(CoxRoomCategory.Combat, 3264, 5280, 2),

    Crabs(CoxRoomCategory.Puzzle, 3264, 5344, 2),
    IceDemon(CoxRoomCategory.Puzzle, 3264, 5344, 0),
    Tightrope(CoxRoomCategory.Puzzle, 3264, 5344, 1),
    Thieving(CoxRoomCategory.Puzzle, 3264, 5376, 0);

    val isCombatOrPuzzle: Boolean
        get() = category == CoxRoomCategory.Combat || category == CoxRoomCategory.Puzzle

    companion object {
        val COMBAT_ROTATIONS: List<List<CoxRoomType>> =
            listOf(
                listOf(Tekton, Vasa, Guardians, Mystics, Shamans, Muttadiles, Vanguards, Vespula),
                listOf(Tekton, Muttadiles, Guardians, Vespula, Shamans, Vasa, Vanguards, Mystics),
                listOf(Vespula, Vanguards, Muttadiles, Shamans, Mystics, Guardians, Vasa, Tekton),
                listOf(Mystics, Vanguards, Vasa, Shamans, Vespula, Guardians, Muttadiles, Tekton),
            )

        val PUZZLES: List<CoxRoomType> = listOf(Crabs, IceDemon, Tightrope, Thieving)

        val FARMING: List<CoxRoomType> = listOf(FarmingFishing, FarmingBats)
    }
}

enum class CoxRoomCategory {
    Start,
    End,
    Scavenger,
    Farming,
    Combat,
    Puzzle,
}

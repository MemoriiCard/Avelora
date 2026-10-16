package org.rsmod.content.minigames.wintertodt

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.rsmod.content.other.pets.Pets

class WintertodtPetTest {
    @Test
    fun `the phoenix pet is a registered pet`() {
        assertNotNull(Pets.forObj(WintertodtLoot.PET))
    }
}

package org.rsmod.content.travel.fairyrings

object FairyRingCode {
    private const val FIRST = "ADCB"
    private const val SECOND = "ILKJ"
    private const val THIRD = "PSRQ"

    fun of(first: Int, second: Int, third: Int): String =
        "${FIRST[first and 3]}${SECOND[second and 3]}${THIRD[third and 3]}"
}

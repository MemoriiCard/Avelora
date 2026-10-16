package org.rsmod.content.raids.toa.invocation

object ToaInvocations {
    const val MAX_LEVEL = 600

    fun raidLevel(active: Set<ToaInvocation>): Int =
        active.sumOf { it.level }.coerceAtMost(MAX_LEVEL)

    fun toggle(active: Set<ToaInvocation>, invocation: ToaInvocation): Set<ToaInvocation> {
        val next = active.toMutableSet()
        if (invocation in next) {
            next -= invocation
            dropOrphans(next)
            return next
        }
        invocation.requires?.let { if (it !in next) return active }
        if (invocation.category.exclusive) next.removeAll { it.category == invocation.category }
        next += invocation
        return next
    }

    fun canEnable(active: Set<ToaInvocation>, invocation: ToaInvocation): Boolean =
        invocation.requires?.let { it in active } ?: true

    private fun dropOrphans(active: MutableSet<ToaInvocation>) {
        do {
            val orphans = active.filter { it.requires != null && it.requires !in active }
            active.removeAll(orphans.toSet())
        } while (orphans.isNotEmpty())
    }

    fun attempts(active: Set<ToaInvocation>): Int? =
        active.firstOrNull { it.category == ToaCategory.Attempts }?.attempts

    fun timeLimit(active: Set<ToaInvocation>): ToaInvocation? =
        active.firstOrNull { it.category == ToaCategory.TimeLimit }

    fun supplyPercent(active: Set<ToaInvocation>): Int =
        active.firstOrNull { it.category == ToaCategory.HelpfulSpirit }?.supplyPercent ?: 100
}

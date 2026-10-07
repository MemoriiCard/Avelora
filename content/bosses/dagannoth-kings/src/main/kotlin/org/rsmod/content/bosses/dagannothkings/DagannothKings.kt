package org.rsmod.content.bosses.dagannothkings

import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript

class DagannothRex @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec = rexSpec()
}

class DagannothPrime @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec = primeSpec()
}

class DagannothSupreme @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec = supremeSpec()
}

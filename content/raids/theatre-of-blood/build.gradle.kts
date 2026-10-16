plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.death)
    implementation(projects.api.invtx)
    implementation(projects.api.script)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.api.random)
    implementation(projects.api.registry)
    implementation(projects.api.route)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.other.pets)
    implementation(projects.content.raids.chambersOfXeric)
    implementation(projects.engine.map)
    implementation(projects.engine.routefinder)
    testImplementation(libs.fastutil)
}

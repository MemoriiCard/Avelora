plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.npc)
    implementation(projects.api.repo)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    testImplementation(projects.engine.map)
    testImplementation(projects.engine.routefinder)
    testImplementation(libs.fastutil)
}

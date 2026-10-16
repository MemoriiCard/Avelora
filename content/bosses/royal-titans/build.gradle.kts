plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.player)
    implementation(projects.api.npc)
    implementation(projects.api.repo)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.instances)
    implementation(projects.api.bossHpBarPlugin)
    testImplementation(libs.fastutil)
    testImplementation(projects.engine.map)
    testImplementation(projects.engine.routefinder)
}

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
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    testImplementation(projects.engine.map)
    testImplementation(libs.fastutil)
}

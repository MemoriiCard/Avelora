plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.death)
    implementation(projects.api.instances)
    implementation(projects.api.invtx)
    implementation(projects.content.skills.slayer)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.repo)
    implementation(projects.api.pluginCommons)
    testImplementation(projects.engine.map)
    testImplementation(libs.fastutil)
}

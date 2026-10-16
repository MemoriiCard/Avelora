plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.invtx)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.random)
    implementation(projects.api.script)
    implementation(projects.api.stats.xpmod)
    implementation(projects.content.skills.smithing)
    testImplementation(libs.fastutil)
    testImplementation(projects.engine.map)
    testImplementation(projects.engine.routefinder)
}

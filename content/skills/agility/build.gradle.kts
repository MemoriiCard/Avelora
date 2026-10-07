plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.config)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.random)
    implementation(projects.api.repo)
    implementation(projects.api.script)
}

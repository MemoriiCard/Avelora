plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(libs.jackson.dataformat.toml)
    implementation(projects.api.invtx)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.random)
    implementation(projects.api.repo)
    implementation(projects.api.script)
}

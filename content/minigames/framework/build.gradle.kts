plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(libs.jackson.dataformat.toml)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.script)
}

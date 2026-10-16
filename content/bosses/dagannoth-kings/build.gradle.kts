plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.pluginCommons)
    testImplementation(projects.engine.map)
    testImplementation(libs.fastutil)
}

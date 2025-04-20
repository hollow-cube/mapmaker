plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":modules:map-core"))
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:terraform")) // Included for schematics, todo both terraform and this module should get schem from central
    implementation(project(":modules:script-engine"))

    implementation(libs.minestom)
    implementation(libs.gson)
    implementation(libs.completely)
    implementation(libs.polar)
    implementation(libs.posthog)
    implementation(libs.bundles.prometheus)
    implementation(libs.bundles.adventure)
}

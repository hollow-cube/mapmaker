plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:common"))
    implementation(project(":modules:compat"))

    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.posthog)
    implementation(libs.gson)
}

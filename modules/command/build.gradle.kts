plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(project(":modules:common"))
    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.posthog)
    implementation(libs.fastutil)
}

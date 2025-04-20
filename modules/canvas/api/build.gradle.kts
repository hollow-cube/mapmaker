plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.posthog)
}

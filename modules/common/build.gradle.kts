plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.gson)
    implementation(libs.caffeine)
    implementation(libs.posthog)
    implementation(libs.fastutil)
    implementation(libs.included.schem)
}

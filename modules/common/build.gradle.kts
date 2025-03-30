plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":modules:datafixerupper"))
    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.gson)
    implementation(libs.posthog)
    implementation(libs.fastutil)
    implementation(libs.kafka)
}

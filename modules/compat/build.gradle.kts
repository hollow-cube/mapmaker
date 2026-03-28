plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(project(":modules:common"))

    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.fastutil)
    implementation(libs.posthog)
    implementation(libs.zstd)
    implementation(libs.feather)
    implementation(libs.apollo)
}

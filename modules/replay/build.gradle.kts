plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(project(":modules:datafix"))
    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.fastutil)
    implementation(libs.zstd)
}

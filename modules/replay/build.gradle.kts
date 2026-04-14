plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.fastutil)
    implementation(libs.zstd)
    implementation(libs.polar) // we just use polar for its network buffer hackery
}

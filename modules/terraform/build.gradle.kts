plugins {
    alias(libs.plugins.javafmt)
    id("mapmaker.java-library")
    id("mapmaker.errorprone")
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.fastutil)
}

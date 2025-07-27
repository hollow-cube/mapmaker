plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(libs.nativeimage)
    implementation(libs.classgraph)
}

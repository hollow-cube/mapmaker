plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":modules:map-core"))

    implementation(libs.minestom)
}

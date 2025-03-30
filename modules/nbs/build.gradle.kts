plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(project(":modules:common"))
    implementation(libs.minestom)
}

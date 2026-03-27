plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:command"))
    implementation(libs.minestom)
}

plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(libs.minestom.testing)
    api(project(":modules:map-core"))
    api(project(":modules:map-runtime")) // needs this for some test input

    implementation(project(":modules:compat"))
//    implementation(project(":modules:script-engine"))
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-standalone"))

    implementation(libs.minestom)
    implementation(libs.junit.api)
}

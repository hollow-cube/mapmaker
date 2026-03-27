plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":modules:test"))
    api(project(":modules:map-core"))

    implementation(project(":modules:compat"))
//    implementation(project(":modules:script-engine"))
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-standalone"))

    implementation(libs.minestom)
    implementation(libs.junit.api)
}

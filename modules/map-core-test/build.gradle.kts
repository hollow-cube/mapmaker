plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":modules:test"))
    api(project(":modules:map-core"))

    // Note that these are also included but not exposed as api. it is valid to use this to test map-core or hub
    // for example neither of which actually include :modules:map directly.
    implementation(project(":modules:map"))
    implementation(project(":modules:compat"))
//    implementation(project(":modules:script-engine"))
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-standalone"))

    implementation(libs.minestom)
    implementation(libs.junit.api)
}

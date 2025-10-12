plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":modules:map-core"))
    api(project(":modules:map-runtime"))
    implementation(project(":modules:compat"))
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:datafix"))

    implementation(libs.minestom)
    implementation(libs.fastutil)
    implementation(libs.polar)
    implementation(libs.completely)
    implementation(libs.bundles.adventure)
    implementation(libs.bundles.otel)

    testImplementation(project(":modules:test"))

}

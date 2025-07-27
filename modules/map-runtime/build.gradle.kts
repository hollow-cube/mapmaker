plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":modules:map-core"))
    api(project(":modules:terraform")) //TODO: this exists for entity implementations, but it shouldn't.
    implementation(project(":modules:datafix"))
    implementation(project(":modules:compat"))

    implementation(libs.minestom)
    implementation(libs.polar)
    implementation(libs.included.molang)
    implementation(libs.bundles.adventure)

    testImplementation(project(":modules:compat"))
    testImplementation(project(":modules:test"))
    testImplementation(libs.bundles.otel)
}

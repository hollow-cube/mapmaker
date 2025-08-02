plugins {
    id("mapmaker.java-library")
    id("mapmaker.packer-data") // TODO(new worlds): remove me later just for testing
}

dependencies {
    api(project(":modules:map-core"))
    api(project(":modules:map")) //TODO: obviously stupid dependency
    api(project(":modules:terraform")) //TODO: this exists for entity implementations, but it shouldn't.
    implementation(project(":modules:datafix"))

    implementation(libs.minestom)
    implementation(libs.polar)

    testImplementation(project(":modules:compat"))
    testImplementation(project(":modules:test"))
    testImplementation(libs.bundles.otel)
}

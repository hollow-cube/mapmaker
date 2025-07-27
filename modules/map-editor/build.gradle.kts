plugins {
    id("mapmaker.java-library")
    id("mapmaker.packer-data") // TODO(new worlds): remove me later just for testing
}

dependencies {
    api(project(":modules:map-core"))
    implementation(project(":modules:compat"))

    implementation(libs.minestom)

    testImplementation(project(":modules:test"))
}

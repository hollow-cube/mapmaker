plugins {
    id("mapmaker.java-library")
    id("mapmaker.packer-data") // TODO(new worlds): remove me later just for testing
}

dependencies {
    api(project(":modules:map-core"))
    api(project(":modules:compat"))

    implementation(libs.minestom)
}

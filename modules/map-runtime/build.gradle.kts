plugins {
    id("mapmaker.java-library")
}

repositories {
    mavenLocal()
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

    implementation(libs.bundles.luau)
    implementation("dev.hollowcube:luau:dev")

    testImplementation(project(":modules:compat"))
    testImplementation(project(":modules:test"))
    testImplementation(libs.bundles.otel)
}

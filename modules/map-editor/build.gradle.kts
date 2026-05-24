plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":modules:map-core"))
    api(project(":modules:map-runtime"))
    implementation(project(":modules:compat"))
    implementation(project(":modules:datafix"))

    implementation(libs.minestom)
    implementation(libs.fastutil)
    implementation(libs.polar)
    implementation(libs.completely)
    implementation(libs.bundles.adventure)
    implementation(libs.bundles.otel) {
        exclude(group = "io.opentelemetry", module = "opentelemetry-exporter-sender-okhttp")
    }
    implementation(libs.included.schem)
    implementation(libs.included.molang)

    // Editor-only Luau tooling: on-demand compilation + hot-reload change source.
    implementation(libs.luau.core)
    implementation(libs.directory.watcher)
    implementation(libs.nats)

    testImplementation(project(":modules:test"))
    testImplementation(project(":modules:map-test"))

}

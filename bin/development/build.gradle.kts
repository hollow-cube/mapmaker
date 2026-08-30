plugins {
    id("mapmaker.java-binary")
    id("mapmaker.packer-data")
}

dependencies {
    implementation(project(":bin:api-server"))
    implementation(project(":bin:api-worker"))
    implementation(project(":modules:api"))
    implementation(project(":bin:config"))
    implementation(project(":bin:hub"))
    implementation(project(":bin:map"))

    implementation(project(":modules:map-core"))
    implementation(project(":modules:map-runtime"))
    implementation(project(":modules:map-editor"))

    implementation(project(":modules:datafix"))
    implementation(project(":modules:terraform"))
    implementation(project(":modules:compat"))

    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.slf4j.jul)
    implementation(libs.logback)
    implementation(libs.polar)
    implementation(libs.bundles.otel) {
        exclude(group = "io.opentelemetry", module = "opentelemetry-exporter-sender-okhttp")
    }
    implementation(libs.bundles.prometheus)
    implementation(libs.fastutil)
    implementation(libs.posthog)
}

application {
    mainClass = "net.hollowcube.mapmaker.dev.DevMain"
}

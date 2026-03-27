plugins {
    id("mapmaker.java-library")
}

repositories {
    maven(url = "https://maven.noxcrew.com/public")
}

dependencies {
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:command"))
    implementation(project(":modules:compat"))
    implementation(project(":modules:common"))
    implementation(project(":modules:datafix"))

    implementation(libs.minestom)
    implementation(libs.bundles.otel) {
        exclude(group = "io.opentelemetry", module = "opentelemetry-exporter-sender-okhttp")
    }
    implementation(libs.bundles.adventure)
    implementation(libs.fastutil)
    implementation(libs.posthog)
    implementation(libs.polar)
    implementation(libs.similarity)
    implementation(libs.completely)
    implementation(libs.gson)
    implementation(libs.jctools)
    implementation(libs.caffeine)
    implementation(libs.bundles.prometheus) // TODO: why is this a core module dependency
    implementation(libs.nats)

    testImplementation(project(":modules:test"))
}

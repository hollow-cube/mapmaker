plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":modules:command"))
    api(project(":modules:common"))
    api(project(":modules:core"))
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:compat"))
    implementation(project(":modules:terraform"))
    implementation(project(":modules:datafix"))

    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.gson)
    implementation(libs.mql)
    implementation(libs.polar)
    implementation(libs.posthog)
    implementation(libs.bundles.otel)
    implementation(libs.bundles.prometheus)
    implementation(libs.fastutil)
    implementation(libs.kafka)
    implementation(libs.slf4j.jul)
    implementation(libs.logback)

    testImplementation(project(":modules:map-core-test"))
}

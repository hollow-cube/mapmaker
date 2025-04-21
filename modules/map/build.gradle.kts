plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":modules:map-core"))
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:compat"))
    implementation(project(":modules:terraform"))
    implementation(project(":modules:replay"))
    implementation(project(":modules:datafix"))
    implementation(project(":modules:script-engine"))

    implementation(libs.minestom)
    implementation(libs.bundles.otel)
    implementation(libs.bundles.adventure)
    implementation(libs.bundles.prometheus)
    implementation(libs.zstd)
    implementation(libs.gson)
    implementation(libs.completely)
    implementation(libs.mql)
    implementation(libs.polar)
    implementation(libs.fastutil)
    implementation(libs.kafka)

    testImplementation(project(":modules:map-core-test"))
}

plugins {
    id("mapmaker.java-binary")
    id("mapmaker.packer-data")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":bin:config"))

    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-standalone"))
    implementation(project(":modules:map-core"))
    implementation(project(":modules:map-runtime"))
    implementation(project(":modules:map-editor"))

    implementation(libs.minestom)
    implementation(libs.polar)
    implementation(libs.bundles.adventure)
    implementation(libs.kafka)

    implementation(libs.slf4j.jul)
    implementation(libs.logback)
    implementation(libs.bundles.otel)
    implementation(libs.bundles.prometheus)
}

application {
    mainClass = "net.hollowcube.mapmaker.map.MapMain"
}

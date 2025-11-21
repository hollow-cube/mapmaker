plugins {
    id("mapmaker.java-binary")
    id("mapmaker.packer-data")
}

dependencies {
    implementation(project(":bin:config"))

    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-standalone"))
    implementation(project(":modules:map-core"))

    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.polar)
    implementation(libs.included.schem)

    implementation(libs.slf4j.jul)
    implementation(libs.logback)
    implementation(libs.bundles.prometheus)
}

application {
    mainClass = "net.hollowcube.mapmaker.hub.HubMain"
}

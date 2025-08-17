plugins {
    id("mapmaker.java-binary")
    id("mapmaker.packer-data")
}

dependencies {
    implementation(project(":bin:config"))

    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-standalone"))
    implementation(project(":modules:core"))
    implementation(project(":modules:map"))
    implementation(project(":modules:terraform"))

    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.slf4j.jul)
    implementation(libs.logback)
    implementation(libs.polar)
    implementation(libs.fastutil)
}

application {
    mainClass = "net.hollowcube.mapmaker.local.LocalServer"
}

plugins {
    id("mapmaker.java-binary")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":modules:map-runtime"))
    implementation(project(":modules:map-editor"))

    implementation(libs.gson)
    implementation(libs.slf4j)
    implementation(libs.logback)
    implementation(libs.otel.api)
    implementation(libs.minestom)
    implementation(libs.luau.core)
}

application {
    mainClass = "net.hollowcube.mapmaker.bundle.BundlerMain"
}

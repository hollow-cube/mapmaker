plugins {
    id("mapmaker.java-binary")
    id("mapmaker.packer-data")

    alias(libs.plugins.blossom)
}

val isRelease = rootProject.properties.getOrDefault("isRelease", "false").toString().toBoolean()

dependencies {
    implementation(project(":bin:config"))

    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-standalone"))
    implementation(project(":modules:core"))
    implementation(project(":modules:hub"))
    implementation(project(":modules:map"))
    implementation(project(":modules:terraform"))
    implementation(project(":modules:script-engine"))

    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.slf4j.jul)
    implementation(libs.logback)
    implementation(libs.polar)
    implementation(libs.bundles.otel)
    implementation(libs.bundles.prometheus)
    implementation(libs.fastutil)
    implementation(libs.kafka)
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("isRelease", isRelease.toString())
            }
        }
    }
}

application {
    mainClass = "net.hollowcube.mapmaker.dev.DevServer"
}
plugins {
    id("mapmaker.java-library")

    alias(libs.plugins.blossom)
}

val isRelease = rootProject.properties.getOrDefault("isRelease", "false").toString().toBoolean()

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:map-core"))

    implementation(libs.minestom)
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

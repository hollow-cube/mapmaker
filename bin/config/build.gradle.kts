plugins {
    id("mapmaker.java-library")

    alias(libs.plugins.blossom)
}

val isRelease = rootProject.properties.getOrDefault("isRelease", "false").toString().toBoolean()

sourceSets {
    main {
        blossom {
            javaSources {
                property("isRelease", isRelease.toString())
            }
        }
    }
}

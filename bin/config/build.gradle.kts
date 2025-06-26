plugins {
    id("mapmaker.java-library")

    alias(libs.plugins.blossom)
}

val isRelease = rootProject.properties.getOrDefault("isRelease", "false").toString().toBoolean()

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:map-core"))
    implementation(project(":modules:datafix"))

    implementation(libs.minestom)
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", System.getenv("MAPMAKER_VERSION") ?: "dev")
                property("commitHash", System.getenv("MAPMAKER_COMMIT_HASH") ?: "dev")
                property("minestomVersion", libs.minestom.get().version)
                property("isRelease", isRelease.toString())
            }
        }
    }
}

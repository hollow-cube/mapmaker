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

                // When building for PRs we embed the rp hash in the build, otherwise its set in the pod spec.
                val staticPackHash = System.getenv("MAPMAKER_RESOURCE_PACK_HASH")
                property(
                    "resourcePackHash",
                    if (staticPackHash != null) "\"${this}\"" else "null"
                )
            }
        }
    }
}

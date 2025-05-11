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
                property("version", "dev") // todo
                property("commit", "dev") // todo
                property("minestom", libs.minestom.get().version)
                property("resourcePackHash", "todo") // todo
                property("isRelease", isRelease.toString())
            }
        }
    }
}

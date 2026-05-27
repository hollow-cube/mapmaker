plugins {
    `kotlin-dsl`
}

repositories {
    // TEMP: consume javafmt from mavenLocal while debugging plugin bugs. Revert to plugin portal once fixes ship.
    mavenLocal {
        content {
            includeGroup("dev.javafmt")
            includeGroup("dev.javafmt.gradle")
        }
    }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.github.node-gradle.node:com.github.node-gradle.node.gradle.plugin:7.1.0")
    implementation("com.gradleup.shadow:com.gradleup.shadow.gradle.plugin:9.0.0-beta12")
    implementation("dev.javafmt.gradle:dev.javafmt.gradle.gradle.plugin:dev")
}

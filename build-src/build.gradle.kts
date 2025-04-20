plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.github.node-gradle.node:com.github.node-gradle.node.gradle.plugin:7.1.0")
    implementation("com.gradleup.shadow:com.gradleup.shadow.gradle.plugin:9.0.0-beta12")
}

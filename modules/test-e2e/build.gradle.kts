import net.fabricmc.loom.task.prod.ClientProductionRunTask
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("mapmaker.java-library")
    id("mapmaker.packer-data")

    alias(libs.plugins.loom)
}

// Includes a dependency as well as its transitive dependencies in the mod.
val includeTransitive: Configuration by configurations.creating {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.8")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.16.14")

    // Resolves to net.fabricmc.fabric-api:fabric-resource-loader-v0:3.1.11+946bf4c3f3
    modImplementation(fabricApi.module("fabric-resource-loader-v0", "0.129.0+1.21.8"))
    modImplementation(fabricApi.module("fabric-client-gametest-api-v1", "0.129.0+1.21.8"))

    implementation(libs.minestom)
    includeTransitive(libs.minestom)
    implementation(project(":modules:hub"))
    includeTransitive(project(":modules:hub"))
    implementation(project(":modules:map-runtime"))
    includeTransitive(project(":modules:map-runtime"))
    implementation(libs.bundles.otel)
    includeTransitive(libs.bundles.otel)
    implementation(project(":modules:compat"))
    includeTransitive(project(":modules:compat"))
    implementation(project(":modules:datafix"))
    includeTransitive(project(":modules:datafix"))
    includeTransitive(project(":bin:config"))
}

tasks.processIncludeJars {
    from(includeTransitive)
}

tasks.register<ClientProductionRunTask>("runGameTests") {
    dependsOn(":bin:packer:buildClient", ":modules:test-e2e:downloadAssets")
    outputs.upToDateWhen { false }

    mods.from(configurations.modImplementation)
    jvmArgs.add("-Dfabric.client.gametest")
    jvmArgs.add("-Dfabric.client.gametest.disableNetworkSynchronizer")

    val gameDirectory = project.layout.buildDirectory.dir("run/clientGameTest")
    runDir.set(gameDirectory)

    doFirst {
        val resourcePackDirectory = gameDirectory.get().dir("resourcepacks")
        resourcePackDirectory.asFile.mkdirs()
        Files.copy(
            rootProject.layout.buildDirectory.dir("client.zip").get().asFile.toPath(),
            resourcePackDirectory.asFile.toPath().resolve("client.zip"),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}

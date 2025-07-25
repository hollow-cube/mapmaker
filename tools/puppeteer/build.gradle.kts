import net.fabricmc.loom.task.prod.ClientProductionRunTask
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("mapmaker.java-library")
    id("mapmaker.packer-data")

    id("fabric-loom") version "1.11-SNAPSHOT"

    id("com.gradleup.shadow") version "9.0.0-beta12"
}

val lib: Configuration by configurations.creating {
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
    lib(libs.minestom)
    implementation(project(":modules:hub"))
    lib(project(":modules:hub"))
    lib(project(":bin:config"))
}

tasks.processIncludeJars {
    from(lib)
}

tasks.register<ClientProductionRunTask>("runGameTests") {
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
            kotlin.io.path.Path("/Users/matt/dev/projects/hollowcube/mapmaker/build/client.zip"),
            resourcePackDirectory.asFile.toPath().resolve("client.zip"),
            StandardCopyOption.REPLACE_EXISTING
        )
//        Files.writeString(
//            gameDirectory.get().file("options.txt").asFile.toPath(),
//            "resourcePacks:[\"vanilla\",\"client.zip\"]"
//        )
    }

}

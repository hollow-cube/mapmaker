import net.fabricmc.loom.task.prod.ClientProductionRunTask
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("mapmaker.java-library")
    id("mapmaker.packer-data")

    id("fabric-loom") version "1.11-SNAPSHOT"
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.8")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.16.14")

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.129.0+1.21.8")
}

tasks.register<ClientProductionRunTask>("runGameTests") {
    outputs.upToDateWhen { false }

//    mods.from(tasks.jar)
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

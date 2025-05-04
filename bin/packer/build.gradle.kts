import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URI

plugins {
    id("mapmaker.java-binary")
}

dependencies {
    implementation(libs.gson)
    implementation(libs.json5)
}

application {
    mainClass = "net.hollowcube.mapmaker.Packer"
}

val mcVersion = libs.versions.minecraft.get()
val packerOut = layout.buildDirectory.dir("packer-out")
val minecraftCache = rootDir.resolve(".gradle").resolve("minecraft-cache").resolve(mcVersion)

tasks.register<DefaultTask>("downloadMinecraft") {
    if (!minecraftCache.exists()) {
        minecraftCache.mkdirs()
        val manifest = getJson("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
        val version = manifest["versions"].asJsonArray.first { element -> element.asJsonObject["id"].asString == mcVersion }
        val versionPackage = getJson(version.asJsonObject.get("url").asString)
        val clientJar = copyTo(versionPackage["downloads"].asJsonObject["client"].asJsonObject["url"].asString, temporaryDir.resolve("client.jar"))

        project.copy {
            from(zipTree(clientJar))
            into(minecraftCache)
            include("assets/**")
        }
    }
}

tasks.register<JavaExec>("runPacker") {
    dependsOn("downloadMinecraft")

    classpath = sourceSets["main"].runtimeClasspath
    mainClass = application.mainClass
    workingDir = rootProject.layout.projectDirectory.asFile
    args = listOf(file(packerOut).absolutePath, minecraftCache.absolutePath)

    inputs.dir(file(rootProject.layout.projectDirectory.dir("resources")))
    outputs.dir(packerOut)

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(23)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

tasks.register<Zip>("buildClient") {
    dependsOn("runPacker")
    from(packerOut.get().dir("client"))

    destinationDirectory = rootProject.layout.buildDirectory
    archiveFileName = "client.zip"
}

configurations.create("packer")

artifacts {
    add("packer", file(packerOut)) {
        builtBy("runPacker")
    }
}

fun getJson(url: String): JsonObject = URI.create(url).toURL().openStream().use { stream ->
    Gson().fromJson(String(stream.readAllBytes()), JsonObject::class.java)
}

fun copyTo(url: String, file: File): File = URI.create(url).toURL().openStream().use { stream ->
    stream.copyTo(file.outputStream())
    file
}
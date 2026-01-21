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

val packerOut = layout.buildDirectory.dir("packer-out")
val minecraftCacheDirectory = rootDir.resolve(".gradle").resolve("minecraft-cache")
val mcVersions = mapOf(
    "1.21.4" to 769,
    "1.21.5" to 770,
    "1.21.6" to 771,
    "1.21.8" to 772,
    "1.21.9" to 773,
)

tasks.register<DefaultTask>("downloadMinecraft") {
    for ((mcVersion, _) in mcVersions) {
        val directory = minecraftCacheDirectory.resolve(mcVersion)
        if (!directory.exists()) {
            val manifest = getJson("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
            val version =
                manifest["versions"].asJsonArray.first { element -> element.asJsonObject["id"].asString == mcVersion }
            val versionPackage = getJson(version.asJsonObject.get("url").asString)
            val clientJar = copyTo(
                versionPackage["downloads"].asJsonObject["client"].asJsonObject["url"].asString,
                temporaryDir.resolve("client.jar")
            )

            project.copy {
                from(zipTree(clientJar))
                into(directory)
                include("assets/**")
            }
        }
    }
}

tasks.register<JavaExec>("runPacker") {
    dependsOn("downloadMinecraft")

    classpath = sourceSets["main"].runtimeClasspath
    mainClass = application.mainClass
    workingDir = rootProject.layout.projectDirectory.asFile
    args = listOf(
        file(packerOut).absolutePath,
        minecraftCacheDirectory.absolutePath,
        mcVersions.map { "${it.key}:${it.value}" }.joinToString(","),
    )

    inputs.dir(file(rootProject.layout.projectDirectory.dir("resources")))
    outputs.dir(packerOut)

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.matching("GraalVM")
        nativeImageCapable = true
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
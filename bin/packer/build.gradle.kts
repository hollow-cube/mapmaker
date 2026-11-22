import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

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
    "1.21.8" to 772,
    "1.21.9" to 773,
    "26.1" to 775,
    "26.2" to 776,
    "26.3" to 777,
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
    workingDir = isolated.rootProject.projectDirectory.asFile
    args = listOf(
        file(packerOut).absolutePath,
        minecraftCacheDirectory.absolutePath,
        mcVersions.map { "${it.key}:${it.value}" }.joinToString(","),
    )

    inputs.dir(file(isolated.rootProject.projectDirectory.dir("resources")))
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

    destinationDirectory = isolated.rootProject.projectDirectory.dir("build")
    archiveFileName = "client.zip"
}

// Writes the longest variant length of every vanilla sound event, for the play sound action's test button.
// Sounds are not in the client jar, so they come from the asset index; only the first and last bytes of each
// ogg are fetched, for the sample rate and the final granule position.
tasks.register("generateSoundDurations") {
    group = "minecraft"
    val mcVersion = providers.gradleProperty("soundsVersion").getOrElse(mcVersions.keys.last())
    val output = rootDir.resolve("modules/map-runtime/src/main/resources/sound_durations.txt")
    doLast {
        val gson = Gson()
        val http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
        fun fetch(url: String, range: String? = null): ByteArray {
            val request = HttpRequest.newBuilder(URI.create(url))
            if (range != null) request.header("Range", "bytes=$range")
            val response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray())
            check(response.statusCode() in 200..299) { "${response.statusCode()} fetching $url" }
            return response.body()
        }
        fun json(url: String) = gson.fromJson(String(fetch(url)), JsonObject::class.java)
        fun objectUrl(hash: String) = "https://resources.download.minecraft.net/${hash.take(2)}/$hash"

        val manifest = json("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
        val version = manifest["versions"].asJsonArray.map { it.asJsonObject }
            .firstOrNull { it["id"].asString == mcVersion } ?: error("unknown version $mcVersion")
        val objects = json(json(version["url"].asString)["assetIndex"].asJsonObject["url"].asString)["objects"].asJsonObject
        val sounds = gson.fromJson(
            String(fetch(objectUrl(objects["minecraft/sounds.json"].asJsonObject["hash"].asString))),
            JsonObject::class.java
        )

        fun soundEntries(event: String) = sounds[event.removePrefix("minecraft:")]?.asJsonObject
            ?.get("sounds")?.asJsonArray?.map { entry ->
                if (entry.isJsonPrimitive) entry.asString to false
                else entry.asJsonObject.let { it["name"].asString to (it["type"]?.asString == "event") }
            } ?: listOf()

        val files = sounds.keySet().flatMap(::soundEntries).filter { !it.second }.map { it.first }.toSet()
        val pool = Executors.newFixedThreadPool(32)
        val fileDurations = try {
            files.associateWith { name ->
                val obj = objects["minecraft/sounds/${name.removePrefix("minecraft:")}.ogg"]?.asJsonObject
                    ?: return@associateWith null
                pool.submit<Long> {
                    val url = objectUrl(obj["hash"].asString)
                    // Vorbis identification header: 27 byte page header, 1 segment entry, then packet type (1),
                    // "vorbis" (6), version (4), channels (1), sample rate (4).
                    val head = fetch(url, "0-63")
                    val sampleRate = ByteBuffer.wrap(head, 40, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    val tail = fetch(url, "-${minOf(obj["size"].asLong, 65536)}")
                    val lastPage = String(tail, Charsets.ISO_8859_1).lastIndexOf("OggS")
                    val granule = ByteBuffer.wrap(tail, lastPage + 6, 8).order(ByteOrder.LITTLE_ENDIAN).long
                    Math.round(granule * 1000.0 / sampleRate)
                }
            }.mapValues { it.value?.get() }
        } finally {
            pool.shutdownNow()
        }

        fun eventDuration(event: String, seen: Set<String>): Long? {
            if (event in seen) return null
            return soundEntries(event).mapNotNull { (name, isEvent) ->
                if (isEvent) eventDuration(name, seen + event) else fileDurations[name]
            }.maxOrNull()
        }

        val lines = sounds.keySet().sorted().mapNotNull { event ->
            eventDuration(event, setOf())?.let { "$event $it" }
        }
        output.writeText(lines.joinToString("\n", postfix = "\n"))
        logger.lifecycle("wrote ${lines.size} sound durations for $mcVersion to $output")
    }
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
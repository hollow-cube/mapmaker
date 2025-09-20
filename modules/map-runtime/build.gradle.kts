import com.google.gson.Gson
import com.google.gson.JsonObject

plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":modules:map-core"))
    api(project(":modules:terraform")) //TODO: this exists for entity implementations, but it shouldn't.
    implementation(project(":modules:datafix"))
    implementation(project(":modules:compat"))

    implementation(libs.minestom)
    implementation(libs.polar)
    implementation(libs.included.molang)
    implementation(libs.bundles.adventure)

    implementation(libs.bundles.luau)

    testImplementation(project(":modules:compat"))
    testImplementation(project(":modules:test"))
    testImplementation(libs.bundles.otel)
}

// Collect all local scripts and add them to the jar
val gson = Gson()
val scriptsDir = rootProject.projectDir.resolve("scripts")
val outPath = layout.buildDirectory.dir("resources/main/net.hollowcube.scripting")

val mapData: List<Pair<String, File>> = scriptsDir.listFiles()
    ?.filter { it.isDirectory }
    ?.mapNotNull { dir ->
        val mapJsonFile = dir.resolve("map.json")
        if (!mapJsonFile.exists()) return@mapNotNull null
        val jsonContent = mapJsonFile.readText()
        val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)
        val mapId = jsonObject.get("id").asString
        return@mapNotNull mapId to dir
    } ?: emptyList()

val zipTasks = mapData.map { (mapId, dir) ->
    tasks.register<Zip>("zip${mapId.toPascalCase()}") {
        archiveFileName.set("${mapId}.zip")
        destinationDirectory.set(outPath)
        from(dir)

        group = "scripting"
    }
}

tasks.named("processResources") {
    dependsOn(zipTasks)
}

fun String.toPascalCase(): String {
    return this.split("-", "_")
        .joinToString("") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}

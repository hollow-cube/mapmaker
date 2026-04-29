plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(project(":tools:lua-slopgen:api"))

    implementation(libs.gson)
    implementation(libs.annotations)
}

/// Custom configuration that aggregates per-engine-module jars containing
/// `META-INF/luau-slopgen/*.json` resources. To register an engine module here:
///
/// ```
/// dependencies {
///     luauApiSpecs(project(":modules:map-runtime"))
/// }
/// ```
val luauApiSpecs: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val engineApiOutputProvider = layout.buildDirectory.file("luau-api/engine-api.json")
val engineApiLockFile = rootProject.layout.projectDirectory.file("engine-api.lock.json").asFile

val aggregateLuauApi = tasks.register<JavaExec>("aggregateLuauApi") {
    group = "verification"
    description = "Aggregate per-library Luau API JSONs and validate cross-module references."

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.hollowcube.luau.docs.task.AggregateMain")
    notCompatibleWithConfigurationCache(
        "JavaExec args are computed from a resolved Configuration at execution time"
    )

    val outputFile = engineApiOutputProvider
    val lockPath = engineApiLockFile.absolutePath
    val specsFiles = luauApiSpecs

    inputs.files(specsFiles)
    outputs.file(outputFile)

    doFirst {
        val jars = specsFiles.resolve().map { it.absolutePath }
        val cliArgs = mutableListOf(
            "--output", outputFile.get().asFile.absolutePath,
            "--lock", lockPath,
        )
        if (jars.isNotEmpty()) {
            cliArgs += "--jars"
            cliArgs += jars
        }
        args = cliArgs
    }
}

val updateLuauApiLock = tasks.register<JavaExec>("updateLuauApiLock") {
    group = "verification"
    description = "Refresh engine-api.lock.json with the current API surface."

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.hollowcube.luau.docs.task.AggregateMain")
    notCompatibleWithConfigurationCache(
        "JavaExec args are computed from a resolved Configuration at execution time"
    )

    val outputFile = engineApiOutputProvider
    val lockPath = engineApiLockFile.absolutePath
    val specsFiles = luauApiSpecs

    inputs.files(specsFiles)

    doFirst {
        val jars = specsFiles.resolve().map { it.absolutePath }
        val cliArgs = mutableListOf(
            "--output", outputFile.get().asFile.absolutePath,
            "--lock", lockPath,
            "--update-lock",
        )
        if (jars.isNotEmpty()) {
            cliArgs += "--jars"
            cliArgs += jars
        }
        args = cliArgs
    }
}

tasks.named("check") {
    dependsOn(aggregateLuauApi)
}

tasks.test {
    System.getProperty("slopgen.update_goldens")?.let {
        systemProperty("slopgen.update_goldens", it)
    }
}

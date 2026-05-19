plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(project(":tools:lua-slopgen"))

    implementation(libs.gson)
    implementation(libs.annotations)
}

/// Custom configuration that aggregates per-engine-module fragment directories. To register an
/// engine module here:
///
/// ```
/// dependencies {
///     luauApiFragments(project(":modules:map-runtime"))
/// }
/// ```
///
/// The module must expose a consumable `luauApiFragments` configuration whose outgoing artifact
/// is its `build/luau-api-fragments` directory (set up by the per-module Gradle wiring).
val luauApiFragments: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    luauApiFragments(project(mapOf("path" to ":modules:map-runtime", "configuration" to "luauApiFragments")))
}

val engineApiOutputDir: DirectoryProperty = project.objects.directoryProperty()
    .convention(layout.buildDirectory.dir("luau-api"))
val engineApiOutputFile: Provider<RegularFile> = engineApiOutputDir.file("engine-api.json")
val engineApiEditorFile: Provider<RegularFile> = engineApiOutputDir.file("engine-api.editor.json")
val luauDeclOutputDir: Provider<Directory> = engineApiOutputDir.dir("types")

/// Configuration-cache-compatible argument providers — they capture lazy properties, not the
/// `Configuration` object, so Gradle can fingerprint and replay them without re-resolving at
/// execution time.

abstract class AggregateArgs : CommandLineArgumentProvider {
    @get:InputFiles
    abstract val fragments: ConfigurableFileCollection
    @get:OutputFile
    abstract val output: RegularFileProperty

    @get:OutputFile
    abstract val editorOutput: RegularFileProperty
    override fun asArguments(): Iterable<String> {
        val args = mutableListOf(
            "--output", output.get().asFile.absolutePath,
            "--editor-output", editorOutput.get().asFile.absolutePath,
        )
        val dirs = fragments.files.map { it.absolutePath }
        if (dirs.isNotEmpty()) {
            args += "--fragments"
            args += dirs
        }
        return args
    }
}

abstract class DiffArgs : CommandLineArgumentProvider {
    @get:InputFile
    abstract val oldFile: RegularFileProperty
    @get:InputFile
    abstract val newFile: RegularFileProperty
    override fun asArguments(): Iterable<String> = listOf(
        "--old", oldFile.get().asFile.absolutePath,
        "--new", newFile.get().asFile.absolutePath,
    )
}

abstract class DeclArgs : CommandLineArgumentProvider {
    @get:InputFile
    abstract val schema: RegularFileProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty
    override fun asArguments(): Iterable<String> = listOf(
        "--schema", schema.get().asFile.absolutePath,
        "--output", output.get().asFile.absolutePath,
    )
}

val aggregateEngineApi = tasks.register<JavaExec>("aggregateEngineApi") {
    group = "verification"
    description = "Aggregate per-library Luau fragment JSONs into a single engine-api.json with " +
            "fully-resolved cross-library type references."

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.hollowcube.luau.engineapi.task.AggregateMain")

    val provider = project.objects.newInstance<AggregateArgs>()
    provider.fragments.from(luauApiFragments)
    provider.output.set(engineApiOutputFile)
    provider.editorOutput.set(engineApiEditorFile)
    argumentProviders.add(provider)
}

val compareEngineApi = tasks.register<JavaExec>("compareEngineApi") {
    group = "verification"
    description = "Diff two engine-api.json snapshots and report breaking changes. Configure " +
            "via `-Pold=path` and `-Pnew=path`."

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.hollowcube.luau.engineapi.task.DiffMain")

    val provider = project.objects.newInstance<DiffArgs>()
    val oldPath = providers.gradleProperty("old")
    val newPath = providers.gradleProperty("new")
    provider.oldFile.set(layout.file(oldPath.map { File(it) }))
    provider.newFile.set(layout.file(newPath.map { File(it) }))
    argumentProviders.add(provider)
}

val buildLuauDeclarations = tasks.register<JavaExec>("buildLuauDeclarations") {
    group = "verification"
    description = "Generate the Luau type bundle (global.d.luau + one require-able .luau per " +
            "library, relative requires) from the aggregated engine API."

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.hollowcube.luau.engineapi.task.DeclMain")

    dependsOn(aggregateEngineApi)

    val provider = project.objects.newInstance<DeclArgs>()
    provider.schema.set(engineApiOutputFile)
    provider.output.set(luauDeclOutputDir)
    argumentProviders.add(provider)
}


tasks.test {
    System.getProperty("slopgen.update_goldens")?.let {
        systemProperty("slopgen.update_goldens", it)
    }
}

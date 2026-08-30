plugins {
    id("mapmaker.java-library")
}

dependencies {
    // Only for `@RuntimeGson`, which is read off the class file rather than loaded, and carrying it
    // at runtime would put minestom and adventure in the api-server's native image for nothing.
    compileOnly(project(":modules:common"))
    annotationProcessor(project(":tools:ipc-gen"))

    // Gson and the JDK http client are what the generated `*Client`/`*Server` classes run on, and
    // otel is how a call on either side of the wire shows up as one trace.
    implementation(libs.gson)
    // A generated server logs what it answered 500 with; the caller only ever sees the message.
    implementation(libs.slf4j)
    api(libs.otel.api)
    api(libs.otel.semconv)
}

// Everything in this module is a wire contract between an api-server deployed from main and game
// servers that ship on release tags and outlive it. The processor writes a descriptor of the whole
// wire into the class output on every compile; `wireGen` commits it, `wireCheck` fails when the
// committed copy has drifted, and `wireCompat` fails when a client at any release tag from
// `wire-baseline` up could not survive this build.
val wireTool: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    wireTool(project(":tools:ipc-gen"))
}

val wireDescriptor = tasks.compileJava.flatMap { it.destinationDirectory.file("wire.json") }
val wireCommitted = layout.projectDirectory.file("wire.json")
val wireBaseline = layout.projectDirectory.file("wire-baseline")

abstract class WireGen : DefaultTask() {
    @get:InputFile
    abstract val generated: RegularFileProperty

    @get:OutputFile
    abstract val committed: RegularFileProperty

    @TaskAction
    fun run() {
        generated.get().asFile.copyTo(committed.get().asFile, overwrite = true)
    }
}

abstract class WireCheck : DefaultTask() {
    @get:InputFile
    abstract val generated: RegularFileProperty

    @get:InputFiles
    abstract val committed: ConfigurableFileCollection

    @TaskAction
    fun run() {
        val expected = generated.get().asFile.readText()
        val actual = committed.singleFile.takeIf { it.exists() }?.readText() ?: ""
        if (expected != actual) {
            throw GradleException("modules/ipc/wire.json is out of date; run `./gradlew :modules:ipc:wireGen` and commit it")
        }
    }
}

val wireGen = tasks.register<WireGen>("wireGen") {
    group = "wire"
    description = "Writes wire.json from what the processor found on the last compile."
    generated = wireDescriptor
    committed = wireCommitted
}

tasks.register<WireCheck>("wireCheck") {
    group = "wire"
    description = "Fails if the committed wire.json differs from what the processor emits."
    mustRunAfter(wireGen)
    generated = wireDescriptor
    committed.from(wireCommitted)
}

tasks.register<JavaExec>("wireCompat") {
    group = "wire"
    description = "Fails if a client at any release tag from wire-baseline up could not survive this build."
    classpath = wireTool
    mainClass = "net.hollowcube.ipc.gen.wire.WireCompat"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    inputs.file(wireDescriptor)
    inputs.file(wireBaseline)
    // The answer depends on the tags in the repository, which no input can describe.
    outputs.upToDateWhen { false }
    args(
        "--current", wireDescriptor.get().asFile.path,
        "--baseline", wireBaseline.asFile.path,
        "--repo", rootProject.layout.projectDirectory.asFile.path,
        "--path", "modules/ipc/wire.json",
    )
}

plugins {
    id("mapmaker.java-library")
}

sourceSets.main {
    // Committed sql-gen output. `sqlGen` rewrites it; `sqlGenCheck` fails CI when it drifts.
    java.srcDir("src/generated/java")
}

dependencies {
    // The database and what every api process is made of — secrets, pools, probes, json logs.
    // What is served, and how, belongs to `bin:api-server`; this has to run embedded in another
    // process with none of that.
    api(project(":tools:sql-gen:runtime"))
    api(project(":modules:ipc"))
    api(libs.hikari)

    implementation(libs.gson)
    implementation(libs.logback)
    // What the api processes publish their events on. Core NATS only; nothing here needs a stream.
    implementation(libs.nats)

    testImplementation(project(":tools:sql-gen:testing"))
}

val sqlGenTool: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    sqlGenTool(project(":tools:sql-gen"))
}

fun sqlGenTask(name: String, vararg extraArgs: String) = tasks.register<JavaExec>(name) {
    group = "sql-gen"
    classpath = sqlGenTool
    mainClass = "net.hollowcube.sqlgen.Main"
    // pglite4j runs Postgres on a WASM runtime and wants the room.
    jvmArgs("-Xmx2g")
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    args(
        "--migrations", layout.projectDirectory.dir("src/main/sql/migrations").asFile.path,
        "--queries", layout.projectDirectory.dir("src/main/sql/queries").asFile.path,
        "--out", layout.projectDirectory.dir("src/generated/java").asFile.path,
        "--package", "net.hollowcube.apiserver.db",
        "--name", "ApiDatabase",
        *extraArgs,
    )
}

sqlGenTask("sqlGen")
sqlGenTask("sqlGenCheck", "--check").configure {
    description = "Fails if the committed generated sources differ from what the generator emits."
}

tasks.withType<Test> {
    jvmArgs("-Xmx2g")
    maxParallelForks = 1
}

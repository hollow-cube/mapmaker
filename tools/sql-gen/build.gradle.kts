plugins {
    id("mapmaker.java-library")
}

sourceSets.test {
    // SampleRoundTripTest drives the sample corpus's committed output, so the golden files are
    // compiled too — which is what proves the emitted code runs, not just that it is stable.
    java.srcDir("sample/generated")
}

dependencies {
    implementation(project(":tools:sql-gen:runtime"))
    implementation(libs.javapoet)
    implementation(libs.pglite4j)

    testImplementation(project(":tools:sql-gen:testing"))
}

tasks.withType<Test> {
    // pglite4j runs Postgres on a WASM runtime; it wants room, and it is single-user.
    jvmArgs("-Xmx2g")
    maxParallelForks = 1

    // GoldenTest reads the sample corpus straight off disk, so gradle has to be told about it or a
    // change to the .sql files leaves the test up-to-date.
    inputs.dir(layout.projectDirectory.dir("sample")).withPathSensitivity(PathSensitivity.RELATIVE)
}

// Rewrites the sample corpus's committed output. Deliberately not part of the test task: the golden
// files are a test source root, so regenerating them cannot depend on the tests compiling first.
tasks.register<JavaExec>("sqlGenSample") {
    group = "sql-gen"
    description = "Regenerates the sample corpus's committed output."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "net.hollowcube.sqlgen.Main"
    jvmArgs("-Xmx2g")
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    args(
        "--migrations", layout.projectDirectory.dir("sample/db/migrations").asFile.path,
        "--queries", layout.projectDirectory.dir("sample/db/queries").asFile.path,
        "--out", layout.projectDirectory.dir("sample/generated").asFile.path,
        "--package", "sample.db",
        "--name", "SampleDatabase",
    )
}

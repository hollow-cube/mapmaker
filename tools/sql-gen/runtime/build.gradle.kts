plugins {
    id("mapmaker.java-library")
}

dependencies {
    // Generated code binds and reads through pgjdbc directly; the runtime only adds the few
    // helpers that would otherwise be copied into every emitted file.
    api(libs.postgresql)
    implementation(libs.slf4j)

    testImplementation(libs.pglite4j)
}

tasks.withType<Test> {
    // pglite4j runs Postgres on a WASM runtime; it wants room, and it is single-user.
    jvmArgs("-Xmx2g")
    maxParallelForks = 1
}

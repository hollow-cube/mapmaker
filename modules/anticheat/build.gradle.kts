plugins {
    id("mapmaker.java-library")
}

dependencies {
    // api: WorldView hands out its chunk map as a Long2ObjectMap.
    api(libs.fastutil)
    implementation(libs.gson)
    implementation(libs.zstd)
}

// Prints a capture trace: `./gradlew :modules:anticheat:dumpTrace -Pfile=trace.trace -Pframes`.
tasks.register<JavaExec>("dumpTrace") {
    group = "anticheat"
    description = "Prints the header, counts and optionally every frame of a capture trace."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "net.hollowcube.anticheat.log.Dump"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    // So -Pfile= takes the path as typed, from wherever the build was invoked.
    workingDir = rootDir
    args(
        providers.gradleProperty("file").getOrElse(""),
        if (providers.gradleProperty("frames").isPresent) "--frames" else "",
    )
}

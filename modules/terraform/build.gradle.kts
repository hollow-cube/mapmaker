plugins {
    alias(libs.plugins.javafmt)
    id("mapmaker.java-library")
    id("mapmaker.errorprone")
    alias(libs.plugins.jmh)
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.fastutil)

    testImplementation(libs.minestomTesting)
    testImplementation(libs.jol.core)
}

jmh {
    fork = 1
    warmupIterations = 2
    warmup = "1s"
    iterations = 3
    timeOnIteration = "1s"
    benchmarkMode = listOf("avgt")
    timeUnit = "ms"
    jvmArgsAppend = listOf("--sun-misc-unsafe-memory-access=allow")
    includes = providers.gradleProperty("jmhIncludes").map { listOf(it) }.orElse(listOf())
    providers.gradleProperty("asyncProfilerLib").orNull?.let { lib ->
        profilers = listOf("async:libPath=$lib;output=collapsed;event=cpu;dir=build/prof")
    }
}

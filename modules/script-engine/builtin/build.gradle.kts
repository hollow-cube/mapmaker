import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("mapmaker.js-module")
}

val isRelease = rootProject.properties.getOrDefault("isRelease", "false").toString().toBoolean()

tasks.register<NpmTask>("tsc") {
    dependsOn(tasks.npmInstall)
    group = "build"
    args.set(listOf("run", if (isRelease) "build:release" else "build"))

    // Caching setup below
    outputs.cacheIf { true }
    inputs.dir(file("src"))
        .withPropertyName("scripts")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files("package.json", "package-lock.json", "tsconfig.json", ".tsbuildinfo")
        .withPropertyName("configFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(layout.projectDirectory.dir("dist"))
        .withPropertyName("dist")
}

tasks.register<NpmTask>("tsc-watch") {
    dependsOn(tasks.npmInstall)
    group = "build"
    args.set(listOf("run", "build:watch"))
}

configurations.create("builtin")

artifacts {
    add("builtin", layout.projectDirectory.dir("dist")) {
        builtBy("tsc")
    }
}

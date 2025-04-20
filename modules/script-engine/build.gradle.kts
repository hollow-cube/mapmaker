plugins {
    id("mapmaker.java-library")
}

val isRelease = rootProject.properties.getOrDefault("isRelease", "false").toString().toBoolean()

val react by configurations.creating
val builtin by configurations.creating

dependencies {
    implementation(project(":modules:common"))

    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.fastutil)
    implementation(libs.gson)
    implementation(libs.posthog)
    api(libs.bundles.graalvm)

    react(project(":modules:script-engine:npm", "react"))
    builtin(project(":modules:script-engine:builtin", "builtin"))
}

tasks.register<Copy>("copyReactLibs") {
    from(react)
    into(layout.buildDirectory.dir("react"))

    eachFile {
        val lastSegment = relativePath.segments.last();
        val remapPath: List<String> = listOf("third_party", "react") +
                relativePath.segments.dropLast(1) +
                listOf(lastSegment.substring(0, lastSegment.indexOf('.')) + ".js")
        relativePath = RelativePath(true, *remapPath.toTypedArray())
    }
}

tasks.register<Copy>("copyBuiltinModule") {
    from(builtin)
    into(layout.buildDirectory.dir("builtin"))

    eachFile {
        val remapPath: List<String> = listOf("builtin") + relativePath.segments
        relativePath = RelativePath(true, *remapPath.toTypedArray())
    }
}

tasks.named("processResources") {
    dependsOn("copyReactLibs")
    if (isRelease) dependsOn("copyBuiltinModule")
}

java {
    sourceSets["main"].resources {
        srcDir(layout.buildDirectory.dir("react"))
        if (isRelease) srcDir(layout.buildDirectory.dir("builtin"))
    }
}

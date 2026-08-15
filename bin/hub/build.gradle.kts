plugins {
    id("mapmaker.java-binary")
    id("mapmaker.packer-data")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":bin:config"))

    implementation(project(":modules:map-core"))
    implementation(project(":modules:map-runtime"))
    implementation(project(":modules:terraform"))
    implementation(project(":modules:compat"))

    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.polar)
    implementation(libs.included.schem)
    implementation(libs.fastutil)

    implementation(libs.slf4j)
    implementation(libs.slf4j.jul)
    implementation(libs.logback)
    implementation(libs.bundles.prometheus)
}

val outPath = layout.buildDirectory.dir("resources/main/net.hollowcube.scripting")

val scriptBundlerClasspath: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    scriptBundlerClasspath(project(":bin:script-bundler"))
}

val bundleHubScripts = tasks.register<JavaExec>("bundleHubScripts") {
    val sourceDir = project.projectDir.resolve("scripts")
    val zipFile = outPath.get().asFile.resolve("hub.zip")

    classpath = scriptBundlerClasspath
    mainClass = "net.hollowcube.mapmaker.bundle.BundlerMain"
    args(
        "--source-dir", sourceDir.absolutePath,
        "--out", zipFile.absolutePath,
    )

    inputs.dir(sourceDir).withPropertyName("hubScriptsSource")
    outputs.file(zipFile).withPropertyName("hubScriptsBundle")

    group = "scripting"
    description = "Compile hub-local Luau scripts and pack them into hub.zip for the hub binary's resources."
}

tasks.named("processResources") {
    dependsOn(bundleHubScripts)
}

application {
    mainClass = "net.hollowcube.mapmaker.hub.HubMain"
}

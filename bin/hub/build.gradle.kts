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

    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-standalone"))
    implementation(project(":modules:map-core"))
    implementation(project(":modules:terraform"))

    compileOnly(project(":tools:lua-slopgen:api"))
    annotationProcessor(project(":tools:lua-slopgen"))

    implementation(libs.luau.core)
    implementation(libs.luau.natives.macos.arm64)
    implementation(libs.luau.natives.linux.x64)
    implementation(libs.luau.natives.windows.x64)

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

val zipHubScripts = tasks.register<Zip>("zipHubScripts") {
    archiveFileName.set("hub.zip")
    destinationDirectory.set(outPath)
    from(project.projectDir.resolve("src/main/resources/scripts"))

    include("**/*.luau")
    include("**/.luaurc")
    exclude(".types")
    exclude(".vscode")

    group = "scripting"
}

tasks.named("processResources") {
    dependsOn(zipHubScripts)
}

application {
    mainClass = "net.hollowcube.mapmaker.hub.HubMain"
}

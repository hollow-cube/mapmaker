import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":modules:map-core"))
    api(project(":modules:terraform")) //TODO: this exists for entity implementations, but it shouldn't.
    implementation(project(":modules:datafix"))
    implementation(project(":modules:compat"))

    implementation(libs.minestom)
    implementation(libs.fastutil)
    implementation(libs.polar)
    implementation(libs.included.molang)
    implementation(libs.bundles.adventure)

    testImplementation(project(":modules:compat"))
    testImplementation(project(":modules:test"))
    testImplementation(project(":modules:map-test"))
    testImplementation(libs.logback)  // BundleModuleLoaderTest uses logback's ListAppender to capture diagnostics
    testImplementation(libs.bundles.otel) {
        exclude(group = "io.opentelemetry", module = "opentelemetry-exporter-sender-okhttp")
    }

    compileOnly(project(":modules:map-runtime-gen:annotations"))
    annotationProcessor(project(":modules:map-runtime-gen"))
    implementation(libs.luau.core)
    if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
        implementation(libs.luau.natives.macos.arm64)
    }
    if (DefaultNativePlatform.getCurrentOperatingSystem().isLinux) {
        implementation(libs.luau.natives.linux.x64)
    }
    if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
        implementation(libs.luau.natives.windows.x64)
    }
}

/// Generated Luau API artifacts (engine-api.json + types/*.luau) are committed under
/// `modules/map-runtime/luau-api/` so PR diffs surface API drift. The annotation processor
/// writes here directly during `compileJava`. The path is required — slopgen fails the build
/// if the option is unset.
val luauApiDir = layout.projectDirectory.dir("luau-api")

tasks.compileJava {
    options.compilerArgs.add("-Aluau.outputDir=${luauApiDir.asFile.absolutePath}")
    outputs.dir(luauApiDir).withPropertyName("luauApi")
}

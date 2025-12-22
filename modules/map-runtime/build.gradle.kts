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
    implementation(libs.directory.watcher)

    testImplementation(project(":modules:compat"))
    testImplementation(project(":modules:test"))
    testImplementation(libs.bundles.otel)

    compileOnly(project(":tools:lua-slopgen:api"))
    annotationProcessor(project(":tools:lua-slopgen"))
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

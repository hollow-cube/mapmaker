plugins {
    id("mapmaker.java-library")
}

dependencies {
    // map-test is a leaf so it can be consumed by both map-runtime and
    // map-editor tests without a project-dependency cycle (map-editor depends
    // on map-runtime). Anything editor-specific (ReloadingScriptSession,
    // ScriptSource, InMemoryScriptSource) stays in map-editor/src/test/.
    api(project(":modules:map-runtime"))
    api(project(":modules:map-core"))
    api(project(":modules:compat"))
    api(project(":modules:test"))

    implementation(libs.minestom)
    implementation(libs.luau.core)

    // JUnit annotations are part of the harness's public surface.
    api(libs.junit.api)
}

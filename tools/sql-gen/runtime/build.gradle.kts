plugins {
    id("mapmaker.java-library")
}

dependencies {
    // Generated code binds and reads through pgjdbc directly; the runtime only adds the few
    // helpers that would otherwise be copied into every emitted file.
    api(libs.postgresql)
}

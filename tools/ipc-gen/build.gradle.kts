plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(libs.javapoet)
    // The wire descriptor is json, written by the processor and read back by `wireCompat`.
    implementation(libs.gson)

    testImplementation(libs.compile.testing)
    // The generated code is written against `modules:ipc`, so the tests compile and run it against
    // the real thing rather than copies that can drift. Not a cycle: `ipc` applies this processor's
    // main output, which never depends on these tests.
    testImplementation(project(":modules:ipc"))
    // `@RuntimeGson` is what marks a record as a wire type, and it lives in `common`, which `ipc`
    // only sees at compile time.
    testImplementation(project(":modules:common"))
}

plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(libs.javapoet)

    testImplementation(libs.compile.testing)
    // The generated code is written against `modules:ipc`, so the tests compile and run it against
    // the real thing rather than copies that can drift. Not a cycle: `ipc` applies this processor's
    // main output, which never depends on these tests.
    testImplementation(project(":modules:ipc"))
    // IpcRoundTripTest drives a generated client against a generated server, so it needs the
    // serializer the generated code uses at runtime.
    testImplementation(libs.gson)
}

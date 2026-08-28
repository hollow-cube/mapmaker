plugins {
    id("mapmaker.java-library")
}

dependencies {
    // Only for `@RuntimeGson`, which is read off the class file rather than loaded, and carrying it
    // at runtime would put minestom and adventure in the api-server's native image for nothing.
    compileOnly(project(":modules:common"))
    annotationProcessor(project(":tools:ipc-gen"))

    // Gson and the JDK http client are what the generated `*Client`/`*Server` classes run on, and
    // otel is how a call on either side of the wire shows up as one trace.
    implementation(libs.gson)
    api(libs.otel.api)
    api(libs.otel.semconv)
}

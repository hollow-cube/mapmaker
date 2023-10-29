plugins {
    `java-library`
}

dependencies {
    api(project(":modules:canvas:api"))
    api(project(":modules:command"))
    implementation(project(":tools:compile"))
    annotationProcessor(project(":tools:compile"))
    testAnnotationProcessor(project(":tools:compile"))

    // Kafka
    api("org.apache.kafka:kafka-clients:3.4.0")

    implementation("org.spongepowered:configurate-yaml:4.1.2")

    implementation(libs.polar)

    implementation("com.squareup.moshi:moshi:1.14.0")

    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    implementation("com.authzed.api:authzed:0.5.0")
    implementation("io.grpc:grpc-protobuf:1.55.1")
    implementation("io.grpc:grpc-stub:1.55.1")
    implementation("com.google.protobuf:protobuf-java:3.24.4")
}


tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xplugin:HollowCubeCompilePlugin")
}

plugins {
    `java-library`
}

dependencies {
    implementation(project(":tools:compile"))
    annotationProcessor(project(":tools:compile"))
    testAnnotationProcessor(project(":tools:compile"))

    // Kafka
    api("org.apache.kafka:kafka-clients:3.4.0")

    implementation("org.spongepowered:configurate-yaml:4.1.2")

    implementation(libs.polar)

    implementation("com.squareup.moshi:moshi:1.14.0")
}


tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xplugin:HollowCubeCompilePlugin")
}

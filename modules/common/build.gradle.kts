plugins {
    `java-library`
}

dependencies {
    api(project(":modules:commonnew"))

    implementation("org.mongodb:mongodb-driver-sync:4.7.0")

    // SpiceDB
    implementation("com.authzed.api:authzed:0.4.0")
    implementation("io.grpc:grpc-protobuf:1.51.1")
    implementation("io.grpc:grpc-stub:1.51.1")
}

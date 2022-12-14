plugins {
    `java-library`
}

dependencies {
    implementation(project(":modules:canvas"))

    implementation("org.mongodb:mongodb-driver-sync:4.7.0")

    // Helidon
    api(platform("io.helidon:helidon-dependencies:3.0.2"))
    api("io.helidon.health:helidon-health")
}

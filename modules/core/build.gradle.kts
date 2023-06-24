plugins {
    `java-library`
}

dependencies {
    // MongoDB
    implementation("org.mongodb:mongodb-driver-sync:4.7.0")

    // Kafka
    api("org.apache.kafka:kafka-clients:3.4.0")

    implementation("org.spongepowered:configurate-yaml:4.1.2")

    implementation(libs.polar)

    implementation("com.squareup.moshi:moshi:1.14.0")
}

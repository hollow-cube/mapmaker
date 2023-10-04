plugins {
    `java-library`
}

dependencies {
    implementation("org.apache.kafka:kafka-clients:3.4.0") // Added for LRU cache
    implementation("net.kyori:adventure-text-minimessage:4.12.0")

    api("com.google.guava:guava:31.1-jre")
    implementation("org.spongepowered:configurate-yaml:4.1.2")
}

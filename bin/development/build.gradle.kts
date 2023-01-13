import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("net.kyori.blossom") version "1.2.0"
}

val minestomVersion = rootProject.property("minestomVersion")
dependencies {
    implementation("com.github.minestommmo:Minestom:${minestomVersion}")

    val commonVersion = rootProject.property("commonVersion")
    implementation("com.github.hollow-cube.common:common:${commonVersion}")
    implementation("com.github.hollow-cube.common:block-placement:${commonVersion}")
    implementation("com.github.hollow-cube.common:instances:${commonVersion}")

    implementation(project(":modules:common"))
    implementation(project(":modules:core"))
    implementation(project(":modules:hub"))
    implementation(project(":modules:map"))
    implementation(project(":modules:terraform"))
    implementation(project(":modules:canvas"))
    implementation(project(":modules:chat"))

    // GRPC Internals for SpiceDB
    implementation("com.authzed.api:authzed:0.4.0")
    implementation("io.grpc:grpc-protobuf:1.51.1")
    implementation("io.grpc:grpc-stub:1.51.1")
    implementation("io.grpc:grpc-core:1.51.1")
    implementation("io.grpc:grpc-netty:1.51.1")
    implementation("io.grpc:grpc-netty-shaded:1.51.1")

    // Helidon
    implementation(platform("io.helidon:helidon-dependencies:3.0.2"))
    implementation("io.helidon.health:helidon-health")
    implementation("io.helidon.metrics:helidon-metrics-prometheus")
    implementation("io.helidon.logging:helidon-logging-slf4j")

    implementation("org.spongepowered:configurate-yaml:4.1.2")

    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("org.slf4j:jul-to-slf4j:2.0.6")
}

application {
    mainClass.set("net.hollowcube.mapmaker.dev.DevServer")
}

tasks.withType<ShadowJar> {
    isZip64 = true
}

blossom {
    replaceToken("{MAPMAKER_VERSION}", "3.0.0-alpha")

    val commitHash = System.getenv("COMMIT_SHA")
    if (commitHash != null) {
        replaceToken("{MAPMAKER_COMMIT}", commitHash.substring(0..6))
    } else {
        replaceToken("{MAPMAKER_COMMIT}", "dev")
    }
    replaceToken("{MINESTOM_VERSION}", minestomVersion)
}

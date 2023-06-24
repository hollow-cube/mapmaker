import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("net.kyori.blossom") version "1.2.0"

    id("com.github.evestera.depsize") version "0.1.0"
}

val minestomVersion = rootProject.property("minestomVersion")
dependencies {

    val commonVersion = rootProject.property("commonVersion")
    implementation("com.github.hollow-cube.common:common:${commonVersion}")
    implementation("com.github.hollow-cube.common:block-placement:${commonVersion}")

    implementation(project(":modules:common"))
    implementation(project(":modules:core"))
    implementation(project(":modules:hub"))
    implementation(project(":modules:map"))
    implementation(project(":modules:canvas:api"))
    runtimeOnly(project(":modules:canvas:impl-standalone"))
    implementation(project(":modules:chat"))
    implementation(project(":modules:terraform"))

    // GRPC Internals for SpiceDB
    implementation("com.authzed.api:authzed:0.4.0")
    implementation("io.grpc:grpc-protobuf:1.51.1")
    implementation("io.grpc:grpc-stub:1.51.1")
    implementation("io.grpc:grpc-core:1.51.1")
    implementation("io.grpc:grpc-netty:1.51.1")
    implementation("io.grpc:grpc-netty-shaded:1.51.1")

    implementation("com.github.mworzala.mc_debug_renderer:minestom:2c354a8e0859b765144d7c629c2a4d62b5f1d220")

    // Helidon
    implementation(platform("io.helidon:helidon-dependencies:3.0.2"))
    implementation("io.helidon.health:helidon-health")
    implementation("io.helidon.metrics:helidon-metrics-prometheus")
    implementation("io.helidon.logging:helidon-logging-slf4j")

    implementation("org.spongepowered:configurate-yaml:4.1.2")

    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("org.slf4j:jul-to-slf4j:2.0.6")

    implementation(libs.minestom)

    implementation("io.pyroscope:agent:0.11.1")
}

sourceSets {
    main {
        resources {
            srcDir("src/main/resources")
            srcDir(rootProject.buildDir.resolve("packer/server"))
        }
    }
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}

application {
    mainClass.set("net.hollowcube.mapmaker.dev.DevServer")
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

    val resourcePackHash = System.getenv("RESOURCE_PACK_SHA")
    if (resourcePackHash != null) {
        replaceToken("{RESOURCE_PACK_SHA}", resourcePackHash)
    } else {
        replaceToken("{RESOURCE_PACK_SHA}", "dev")
    }
}

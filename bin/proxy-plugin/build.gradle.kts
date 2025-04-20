plugins {
    // Not actually a binary but we use java-binary so that shadowjar is applied.
    id("mapmaker.java-binary")
}

repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(libs.velocity.api) {
        exclude(group = "net.kyori")
    }
    implementation(libs.kafka)

    implementation(libs.bundles.adventure)
}

application {
    mainClass = "not.a.main"
}

tasks.shadowJar {
    dependencies {
        include("org.apache.kafka:kafka-clients")
    }
}

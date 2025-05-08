plugins {
    id("mapmaker.java-library")
}

repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    annotationProcessor(libs.velocity.api)
    implementation(libs.velocity.api) {
        exclude(group = "net.kyori")
    }
    implementation(libs.bundles.adventure)
}

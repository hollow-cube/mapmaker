plugins {
    java
    application
    id("com.gradleup.shadow")
}

group = "net.hollowcube"

repositories {
    if (libs.minestom.get().version == "dev" || libs.polar.get().version == "dev" || true)
        mavenLocal()
    mavenCentral()

    maven(url = "https://maven.noxcrew.com/public") {
        content {
            includeGroup("com.noxcrew.noxesium")
        }
    }
}

dependencies {
    implementation(libs.autoservice.annotations)
    annotationProcessor(libs.autoservice)

    implementation(libs.annotations)
    implementation(libs.slf4j)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

tasks.shadowJar {
    mergeServiceFiles()
}

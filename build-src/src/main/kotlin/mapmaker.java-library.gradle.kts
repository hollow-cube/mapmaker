plugins {
    `java-library`
}

repositories {
    if (libs.minestom.get().version == "dev")
        mavenLocal()
    mavenCentral()

    maven(url = "https://maven.noxcrew.com/public") {
        content {
            includeGroup("com.noxcrew.noxesium")
        }
    }

    maven(url = "https://repo.feathermc.net/artifactory/maven-releases") {
        content {
            includeGroup("net.digitalingot.feather-server-api")
        }
    }

    maven(url = "https://dist.labymod.net/api/v1/maven/release/") {
        content {
            includeGroup("net.labymod.serverapi")
            includeGroup("net.labymod.serverapi.integration")
        }
    }
}

dependencies {
    implementation(libs.autoservice.annotations)
    annotationProcessor(libs.autoservice)

    implementation(libs.annotations)
    implementation(libs.slf4j)

    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.engine)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

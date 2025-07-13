plugins {
    `java-library`
}

repositories {
    // This code is duplicated in the java-binary configuration, should make any changes there also.
    val centralLibs = listOf(libs.minestom, libs.polar, libs.posthog, libs.adventure.api)
            .mapNotNull { it.get().version }
    if (centralLibs.any { it == "dev" })
        mavenLocal()
    if (centralLibs.any { it.endsWith("-SNAPSHOT") || it.matches(Regex("^.+-(\\d{8})\\.(\\d{6})-(\\d+)\$")) }) {
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/") {
            content {
                includeGroup("net.minestom")
                includeGroup("dev.hollowcube")
            }
        }
    }

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

    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.engine)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

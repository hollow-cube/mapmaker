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

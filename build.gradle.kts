plugins {
    id("net.ltgt.errorprone") version "3.1.0" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("org.sonarqube") version "3.5.0.2730"
}

sonarqube{
    properties{
        property("sonar.host.url", "https://sonarqube.hollowcube.dev")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.projectName", "mapmaker")
        property("sonar.projectKey", "mapmaker")
    }
}

allprojects {
    tasks.withType<Copy> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        jvmArgs(
                "--enable-preview",
                "--add-modules", "jdk.incubator.concurrent",
        )
    }
}

subprojects {
    sonarqube{
        properties {
            property("sonar.sources", "src/main/")
        }
    }
}

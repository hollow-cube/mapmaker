import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

subprojects {
    group = "net.hollowcube.mapmaker"

    val isStandalone = project.path.replace(":modules", "") in setOf(
        ":canvas"
    )
    if (isStandalone) {
        return@subprojects
    }

    apply(plugin = "net.ltgt.errorprone")
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        maven(url = "https://jitpack.io")
    }

    dependencies {
        // A bug with kotlin dsl
        val compileOnly by configurations
        val implementation by configurations
        val annotationProcessor by configurations
        val testImplementation by configurations
        val testAnnotationProcessor by configurations
        val errorprone by configurations

        // Errorprone
        errorprone("com.google.errorprone:error_prone_core:2.14.0")
        errorprone("com.uber.nullaway:nullaway:0.9.8")
        implementation("com.google.errorprone:error_prone_annotations:2.14.0")

        // Junit 5, parameters, truth
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
        testImplementation("com.google.truth:truth:1.1.3")

        implementation("com.google.code.gson:gson:2.10.1")

        // TestContainers
        fun testContainersApi(name: String) {
            testImplementation("org.testcontainers:$name:1.17.3") {
                exclude(group = "junit", module = "junit")
            }
        }
        testContainersApi("testcontainers")
        testContainersApi("junit-jupiter")
        testContainersApi("mongodb")

        // Auto service (SPI)
        annotationProcessor("com.google.auto.service:auto-service:1.0.1")
        testAnnotationProcessor("com.google.auto.service:auto-service:1.0.1")
        implementation("com.google.auto.service:auto-service-annotations:1.0.1")

        // Prometheus
        val prometheusVersion = rootProject.property("prometheusVersion")
        implementation("io.prometheus:simpleclient:${prometheusVersion}")

        // Minestom
        compileOnly("com.github.hollow-cube.Minestom:Minestom:${rootProject.property("minestomVersion")}")
        testImplementation("com.github.hollow-cube.Minestom:Minestom:${rootProject.property("minestomVersion")}")
        testImplementation("com.github.hollow-cube.Minestom:testing:${rootProject.property("minestomVersion")}")

        if (project.name != "common") {
            implementation(project(":modules:common"))
        }
//        implementation("com.github.hollow-cube.common:common:${rootProject.property("commonVersion")}")
//        testImplementation("com.github.hollow-cube.common:test:${rootProject.property("commonVersion")}")
    }

    tasks.getByName<Test>("test") {
        useJUnitPlatform()
    }

    tasks.withType<JavaCompile> {
        options.errorprone.disableWarningsInGeneratedCode.set(true)
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            check("UnusedMethod", CheckSeverity.OFF) // Does not play well with canvas
            check("UnusedVariable", CheckSeverity.OFF) // Does not play well with canvas, has a bug with records
            option("NullAway:AnnotatedPackages", "com.uber")
        }
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
    }
}
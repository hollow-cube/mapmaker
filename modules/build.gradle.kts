import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

subprojects {
    apply(plugin = "java")
    apply(plugin = "net.ltgt.errorprone")
    apply(plugin = "maven-publish")

    group = "net.hollowcube.mapmaker"

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

        errorprone("com.google.errorprone:error_prone_core:2.14.0")
        errorprone("com.uber.nullaway:nullaway:0.9.8")
        implementation("com.google.errorprone:error_prone_annotations:2.14.0")

        // Auto service (SPI)
        annotationProcessor("com.google.auto.service:auto-service:1.0.1")
        testAnnotationProcessor("com.google.auto.service:auto-service:1.0.1")
        implementation("com.google.auto.service:auto-service-annotations:1.0.1")

        // Prometheus
        val prometheusVersion = rootProject.property("prometheusVersion")
        implementation("io.prometheus:simpleclient:${prometheusVersion}")

        // Minestom
        compileOnly("com.github.hollow-cube:Minestom:${rootProject.property("minestomVersion")}")

        if (project.name != "common") {
            implementation(project(":modules:common"))
        }
//        implementation("com.github.hollow-cube.common:common:${rootProject.property("commonVersion")}")
        testImplementation("com.github.hollow-cube.common:test:${rootProject.property("commonVersion")}")
    }

    tasks.getByName<Test>("test") {
        useJUnitPlatform()
    }

    tasks.withType<JavaCompile> {
        options.errorprone.disableWarningsInGeneratedCode.set(true)
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
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
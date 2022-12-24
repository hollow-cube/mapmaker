import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("net.kyori.blossom") version "1.2.0"
}

val minestomVersion = rootProject.property("minestomVersion")
dependencies {
    implementation("com.github.minestommmo:Minestom:${minestomVersion}")

    val commonVersion = rootProject.property("commonVersion")
    implementation("com.github.hollow-cube.common:common:${commonVersion}")
    implementation("com.github.hollow-cube.common:block-placement:${commonVersion}")
    implementation("com.github.hollow-cube.common:instances:${commonVersion}")

    implementation(project(":modules:common"))
    implementation(project(":modules:hub"))
    implementation(project(":modules:map"))
    implementation(project(":modules:terraform"))
    implementation(project(":modules:canvas"))
    implementation(project(":modules:chat"))

    implementation("org.yaml:snakeyaml:1.33")
}

application {
    mainClass.set("net.hollowcube.mapmaker.dev.DevServer")
}

tasks.withType<ShadowJar> {
    isZip64 = true
}

blossom {
    //todo manage these better
    replaceToken("{MAPMAKER_VERSION}", "3.0.0-alpha")

    val commitHash = System.getenv("COMMIT_SHA")
    if (commitHash != null) {
        replaceToken("{MAPMAKER_COMMIT}", commitHash.substring(0..6))
    } else {
        replaceToken("{MAPMAKER_COMMIT}", "dev")
    }
    replaceToken("{MINESTOM_VERSION}", minestomVersion)
}

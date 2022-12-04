import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
}

dependencies {
    implementation("com.github.minestommmo:Minestom:${rootProject.property("minestomVersion")}")

    val commonVersion = rootProject.property("commonVersion")
    implementation("com.github.hollow-cube.common:common:${commonVersion}")

    implementation(project(":modules:common"))
    implementation(project(":modules:hub"))
    implementation(project(":modules:map"))
}

application {
    mainClass.set("net.hollowcube.mapmaker.dev.DevServer")
}

tasks.withType<ShadowJar> {
    isZip64 = true
}

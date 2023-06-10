import org.gradle.internal.deployment.RunApplication

plugins {
    application
}

dependencies {
    // We will use json5 to remove comments from JSON, but moshi for the actual parsing
    implementation("de.marhali:json5-java:2.0.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.jetbrains:annotations:24.0.1")


}

application {
    mainClass.set("net.hollowcube.mapmaker.Packer")
}

tasks.getByName<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

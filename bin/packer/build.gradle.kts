plugins {
    id("mapmaker.java-binary")
}

dependencies {
    implementation(libs.gson)
    implementation(libs.json5)
}

application {
    mainClass = "net.hollowcube.mapmaker.Packer"
}

val packerOut = layout.buildDirectory.dir("packer-out");

tasks.register<JavaExec>("runPacker") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = application.mainClass
    workingDir = rootProject.layout.projectDirectory.asFile
    args = listOf(file(packerOut).absolutePath)

    inputs.dir(file(rootProject.layout.projectDirectory.dir("resources")))
    outputs.dir(packerOut)

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(23)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

tasks.register<Zip>("buildClient") {
    dependsOn("runPacker")
    from(packerOut.get().dir("client"))

    destinationDirectory = rootProject.layout.buildDirectory
    archiveFileName = "client.zip"
}

configurations.create("packer")

artifacts {
    add("packer", file(packerOut)) {
        builtBy("runPacker")
    }
}

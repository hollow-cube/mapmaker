plugins {
    id("mapmaker.java-binary")
    id("mapmaker.packer-data")
    id("org.graalvm.buildtools.native") version "0.10.6"
}

dependencies {
    implementation(project(":bin:config"))

    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-standalone"))
    implementation(project(":modules:core"))
    implementation(project(":modules:datafix"))
    implementation(project(":modules:terraform"))
    implementation(project(":modules:map-runtime"))

    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.slf4j.jul)
    implementation(libs.logback)
    implementation(libs.bundles.prometheus)
    implementation(libs.bundles.otel)

    nativeImageCompileOnly(project(":tools:native-image-helper"))
    configurations.named("nativeImageClasspath") {
        exclude(group = "net.minestom", module = "data")
    }
}

val extractMinestomData by tasks.registering(Copy::class) {
    val jarFile = configurations.runtimeClasspath.get()
        .resolvedConfiguration
        .resolvedArtifacts
        .find {
            it.moduleVersion.id.group == "net.minestom" && it.name == "data"
        }?.file ?: throw GradleException("net.minestom:data not found in runtimeClasspath")

    from(zipTree(jarFile))
    into(layout.buildDirectory.dir("minestom-data"))
}

tasks.nativeCompile {
    dependsOn(extractMinestomData)
}

application {
    mainClass = "net.hollowcube.mapmaker.isolate.IsolateMain"
}

graalvmNative {
    binaries {
        named("main") {
            fallback.set(false)
            buildArgs(
                listOf(
                    "--enable-native-access=ALL-UNNAMED", "--enable-monitoring=jfr",
                    "-H:+UnlockExperimentalVMOptions", "-H:+ForeignAPISupport",
                    "--features=net.hollowcube.nativeimage.HCNativeImageFeature",
                    "--static-nolibc", "--no-fallback",
                    "--emit build-report",
                )
            )
        }
    }
}

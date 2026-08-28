plugins {
    id("mapmaker.java-binary")
    id("org.graalvm.buildtools.native") version "0.10.6"
}

dependencies {
    implementation(project(":modules:api"))

    implementation(libs.gson)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.slf4j)
    implementation(libs.slf4j.jul)
    implementation(libs.logback)

    testImplementation(libs.junit.api)
    testImplementation(libs.junit.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass = "net.hollowcube.apiserver.Main"
}

graalvmNative {
    // pgjdbc and Hikari both need reflection config that neither ships in its own jar.
    metadataRepository {
        enabled = true
    }

    // Both G1 and static linking are linux-only in native-image, and CI is the only place this is
    // built for real; a local build falls back to the serial collector and dynamic linking.
    val linux = System.getProperty("os.name").startsWith("Linux")

    binaries {
        named("main") {
            fallback = false
            buildArgs(
                listOfNotNull(
                    // Oracle GraalVM only, which is what CI builds in; community edition does not
                    // know the option and a local build has to drop it.
                    "--emit build-report",

                    // The only reflection in the image is gson's, so the reachability metadata in
                    // src/main/resources covers it; this is the one class the logger cannot be
                    // looked up through at runtime.
                    "--initialize-at-build-time=ch.qos.logback.classic.spi.LogbackServiceProvider",

                    // A request-serving process wants G1's pauses over the serial collector's, and
                    // the heap it sizes itself against is the container memory limit the chart sets.
                    // G1 also raises the glibc the binary needs to 2.38, which is what pins the
                    // runtime image to distroless/base-debian13 rather than -12.
                    "--gc=G1".takeIf { linux },

                    // Static except for libc, which stays dynamic and so has to be no older in the
                    // runtime image than in the build one.
                    "--static-nolibc".takeIf { linux },
                )
            )
        }
    }
}

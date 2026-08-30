plugins {
    id("mapmaker.java-binary")
    // The actions build their editor sprites up front, against the packer's sprites.json, so a
    // process that only ever decodes them still needs it on the classpath.
    id("mapmaker.packer-data")
}

// A jar, not a native image: this is a long-running consumer, so startup time and memory floor
// buy it nothing and the JIT is what its cpu-bound jobs want. It is also what keeps its build to a
// minute on every push, now that its inputs include most of the runtime.
dependencies {
    implementation(project(":modules:api"))

    // Indexing decodes trigger data with the codecs the runtime uses, which reach into Minestom's
    // registries, so the whole runtime comes along; none of it is ever ticked.
    implementation(project(":modules:common"))
    implementation(project(":modules:core"))
    implementation(project(":modules:datafix"))
    implementation(project(":modules:map-core"))
    implementation(project(":modules:map-runtime"))

    implementation(libs.minestom)
    implementation(libs.polar)
    implementation(libs.fastutil)
    implementation(libs.adventure.nbt)
    implementation(libs.otel.api)

    implementation(libs.gson)
    implementation(libs.postgresql)
    implementation(libs.posthog)
    implementation(libs.slf4j)
    implementation(libs.slf4j.jul)
    implementation(libs.logback)

    testImplementation(project(":tools:sql-gen:testing"))
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // The runner tests run on pglite4j, which wants the room and one instance per JVM.
    jvmArgs("-Xmx2g")
    maxParallelForks = 1
}

application {
    mainClass = "net.hollowcube.apiworker.Main"
    // Named rather than `logback.xml`, so that a process which puts this on its classpath alongside
    // its own — the development server hosts both of these — does not end up with several and a
    // warning about which one won. The image passes logback-prod.xml the same way.
    applicationDefaultJvmArgs = listOf("-Dlogback.configurationFile=logback-local.xml")
}

import java.net.URI
import java.security.MessageDigest

plugins {
    id("mapmaker.java-library")
    // Newer than bin/api-server's pin on purpose: relocation remaps every class through ASM, and
    // only 9.3+ bundles an ASM that reads Java 25 class files.
    id("com.gradleup.shadow") version "9.3.1"
}

repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Velocity is on the proxy's own classpath, so it must never be shaded into the plugin jar.
    // velocity-proxy (needed for ConnectedPlayer#getConnection) is not published to the papermc
    // repo, only velocity-api/-annotation-processor/-brigadier/-native, and the only copy of it is
    // the fat jar a local velocity run drops in scratch/proxy/ - absent in CI, and a second copy of the
    // velocity-api classes on the compile classpath if present. So it is never on the classpath:
    // VelocityInternals reaches ConnectedPlayer#getConnection()#getChannel() through MethodHandles.
    annotationProcessor(libs.velocity.api)
    compileOnly(libs.velocity.api)

    // The session count on the server list ping and the trace store the shipper puts to are ipc
    // services, so the generated SessionClient and AnticheatClient ride along. They drag in
    // gson (which velocity ships its own copy of; the shaded one is never loaded) and
    // opentelemetry-api/-semconv, which is only ever OpenTelemetry.noop() here, since the proxy has
    // no sdk and nothing on the proxy reads a span.
    implementation(project(":modules:ipc"))
    // The control channel's messages are gson too, like the trace meta the ipc client encodes.
    implementation(libs.gson)

    // Netty is the proxy's, same as velocity: compileOnly for the tap, never shaded.
    compileOnly(libs.netty.transport)
    compileOnly(libs.netty.codec)
    compileOnly(libs.netty.handler)

    implementation(project(":modules:anticheat"))

    // The same simpleclient the backend servers use; ProxyHttpServer serves it on /metrics.
    implementation(libs.prometheus)
    implementation(libs.prometheus.httpserver)

    // Tap tests drive an EmbeddedChannel; velocity-api brings no netty of its own.
    testImplementation(libs.netty.transport)
    testImplementation(libs.netty.codec)
    testImplementation(libs.netty.handler)

    // Reading the real-client capture fixtures the tap tests replay.
    testImplementation(libs.gson)
    testImplementation(libs.zstd)
}

// The commit the plugin was built from, stamped into the jar as a resource: ProxyPlugin reads it
// into PROXY_VERSION and it lands in every trace header and store row.
val gitCommit = providers.exec { commandLine("git", "rev-parse", "--short=10", "HEAD") }
    .standardOutput.asText.map(String::trim)
val buildStamp = layout.buildDirectory.file("generated/build-stamp/hc-proxy-build")
val writeBuildStamp = tasks.register("writeBuildStamp") {
    inputs.property("commit", gitCommit)
    outputs.file(buildStamp)
    val file = buildStamp
    val commit = gitCommit
    doLast { file.get().asFile.writeText(commit.get()) }
}
tasks.processResources {
    from(writeBuildStamp)
}

// The tap replays the very same real-client 776 captures the engine tests do. They live with the
// module that owns their format, so they are read from there rather than checked in twice.
tasks.processTestResources {
    from(rootDir.resolve("modules/anticheat/src/test/resources/fixtures")) {
        into("fixtures")
    }
}

// The proxy loads a single jar, so the plugin ships fat; the plain jar keeps a classifier so both
// can live in build/libs and the shaded one keeps the name stageProxy copies.
tasks.jar {
    archiveClassifier = "thin"
}

tasks.shadowJar {
    archiveClassifier = ""

    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Velocity bundles a minimized fastutil (only the classes it uses) and plugin loading is
    // parent-first, so an unrelocated fastutil call resolves against velocity's cut and dies on
    // any class it stripped (ObjectArraySet, from Int2ObjectMaps.EmptyMap, was the first).
    relocate("it.unimi.dsi.fastutil", "net.hollowcube.proxy.shaded.fastutil")

    // Velocity injects its own org.slf4j.Logger; a second copy in the plugin jar would be a
    // different class if the plugin loader ever stopped delegating parent-first.
    dependencies {
        exclude(dependency("org.slf4j:slf4j-api"))
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

// The velocity build the proxy image runs, pinned by the sha256 fill.papermc.io publishes for it
// (https://fill.papermc.io/v3/projects/velocity/versions/<version>/builds/latest); bump the three
// together. libs.versions.toml's velocity is the api the plugin compiles against, which is this
// build's version line.
val velocityBuild = "3.5.0-SNAPSHOT-609"
val velocitySha256 = "0c3d16b70ed757638b696a9a87d670b4301f23a6fef30c3acbbd9b0e0d7b29bb"
val velocityUrl = "https://fill-data.papermc.io/v1/objects/$velocitySha256/velocity-$velocityBuild.jar"

val velocityJar = layout.buildDirectory.file("proxy/velocity-$velocityBuild.jar")

val downloadProxy = tasks.register("downloadProxy") {
    group = "proxy"
    description = "Downloads the pinned velocity build into build/proxy and checks its sha256."
    inputs.property("url", velocityUrl)
    inputs.property("sha256", velocitySha256)
    outputs.file(velocityJar)
    // Locals: an action that reads a script-level val captures the script, which the
    // configuration cache refuses.
    val url = velocityUrl
    val sha = velocitySha256
    val jarFile = velocityJar
    doLast {
        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(1 shl 16)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    digest.update(buf, 0, n)
                }
            }
            return digest.digest().joinToString("") { b -> "%02x".format(b) }
        }

        val jar = jarFile.get().asFile
        if (jar.exists() && sha256(jar) == sha) return@doLast
        jar.parentFile.mkdirs()
        val part = File(jar.path + ".part")
        val connection = URI(url).toURL().openConnection()
        connection.setRequestProperty("User-Agent", "mapmaker (+https://github.com/hollow-cube/mapmaker)")
        connection.getInputStream().use { input -> part.outputStream().use { output -> input.copyTo(output) } }
        val got = sha256(part)
        check(got == sha) { "velocity jar sha256 mismatch: expected $sha, got $got" }
        check(part.renameTo(jar)) { "could not move ${part.name} into place" }
    }
}

// The proxy image's /app, exactly: proxy/ (config, via jars, icon), the velocity jar and the
// plugin. The Dockerfile copies this directory and nothing else.
val stageProxy = tasks.register<Sync>("stageProxy") {
    group = "proxy"
    description = "Assembles the proxy's runtime directory in build/proxy/stage."
    into(layout.buildDirectory.dir("proxy/stage"))
    from(layout.projectDirectory.dir("proxy"))
    from(downloadProxy) { rename { "velocity.jar" } }
    from(tasks.shadowJar) { into("plugins") }
}

// A local run of the very thing that ships, differing only in what has to differ: the hub to
// forward to, the bind port, and the forwarding secret, each of which is a gradle property.
// Velocity's own state (logs, lang, plugin data) is left alone between runs.
//
//   ./gradlew :bin:proxy-plugin:runProxy -PproxyHub=127.0.0.1:25565 -PproxySecret=abcdef
val proxyRunDir = layout.buildDirectory.dir("proxy/run")
val prepareProxyRun = tasks.register<Sync>("prepareProxyRun") {
    group = "proxy"
    description = "Stages build/proxy/run for runProxy, with the dev overrides applied."
    into(proxyRunDir)
    from(stageProxy)
    preserve {
        include("logs/**", "lang/**")
        include("plugins/*/**")
    }

    val hub = providers.gradleProperty("proxyHub").getOrElse("127.0.0.1:25565")
    val bind = providers.gradleProperty("proxyBind").getOrElse("0.0.0.0:25577")
    val secret = providers.gradleProperty("proxySecret").getOrElse("abcdef")
    val cookieSecret = providers.gradleProperty("proxyCookieSecret").getOrElse("local dev")
    inputs.property("hub", hub)
    inputs.property("bind", bind)
    inputs.property("secret", secret)
    inputs.property("cookieSecret", cookieSecret)
    val runDir = proxyRunDir
    doLast {
        val dir = runDir.get().asFile
        val config = dir.resolve("velocity.toml")
        config.writeText(config.readText()
            .replace(Regex("(?m)^bind = \".*\"$"), "bind = \"$bind\"")
            .replace(Regex("(?m)^anyhub = \".*\"$"), "anyhub = \"$hub\""))
        dir.resolve("forwarding.secret").writeText(secret)
        dir.resolve("cookie.secret").writeText(cookieSecret)
    }
}

tasks.register<JavaExec>("runProxy") {
    group = "proxy"
    description = "Runs the staged proxy locally, in front of -PproxyHub (127.0.0.1:25565)."
    dependsOn(prepareProxyRun)
    workingDir = proxyRunDir.get().asFile
    classpath = files(proxyRunDir.map { it.file("velocity.jar") })
    mainClass = "com.velocitypowered.proxy.Velocity"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    jvmArgs("-Xms512M", "-Xmx512M", "-Dvelocity.max-plugin-message-payload-size=1048576")
    // The same http side the deployment drives, on 9125 so a dev server's 9124 is free.
    environment("PROXY_HTTP_PORT", providers.gradleProperty("proxyHttpPort").getOrElse("9125"))
    // Where the ipc services are served from: a local api-server, or DevServer embedding them.
    environment("IPC_SERVICE_URL", providers.gradleProperty("proxyIpcUrl").getOrElse("http://127.0.0.1:9124"))
    // The go api-server the compose stack publishes on 9127; logins need it for session rows.
    environment("SESSION_SERVICE_URL", providers.gradleProperty("proxySessionUrl").getOrElse("http://127.0.0.1:9127"))
    environment("ANTICHEAT_ENABLED", "true")
    environment("PROXY_COOKIE_SECRET_FILE", proxyRunDir.get().asFile.resolve("cookie.secret").path)
    standardInput = System.`in`
}

// The trace store for dev and for the e2e harness: the generated ipc server over a directory, so
// a proxy can ship somewhere real without an api-server. StubStore lives in the test source set
// because it is the very store TraceShipperTest ships to.
//
//   ./gradlew :bin:proxy-plugin:runStubStore -Pport=9126 -Pdir=scratch/proxy/store
tasks.register<JavaExec>("runStubStore") {
    group = "anticheat"
    description = "Serves AnticheatService out of a directory, for dev and the e2e harness."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "net.hollowcube.proxy.anticheat.StubStore"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    // So -Pdir= takes the path as typed, from wherever the build was invoked.
    workingDir = rootDir
    args(
        providers.gradleProperty("port").getOrElse("9126"),
        providers.gradleProperty("dir").getOrElse("scratch/proxy/store"),
    )
}

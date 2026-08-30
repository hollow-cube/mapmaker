import java.net.URI
import java.security.MessageDigest

plugins {
    id("mapmaker.java-library")
}

repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    annotationProcessor(libs.velocity.api)
    implementation(libs.velocity.api)
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
    inputs.property("hub", hub)
    inputs.property("bind", bind)
    inputs.property("secret", secret)
    val runDir = proxyRunDir
    doLast {
        val dir = runDir.get().asFile
        val config = dir.resolve("velocity.toml")
        config.writeText(config.readText()
            .replace(Regex("(?m)^bind = \".*\"$"), "bind = \"$bind\"")
            .replace(Regex("(?m)^anyhub = \".*\"$"), "anyhub = \"$hub\""))
        dir.resolve("forwarding.secret").writeText(secret)
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
    environment("METRICS_PORT", providers.gradleProperty("proxyHttpPort").getOrElse("9125"))
    // Velocity's own /server, hidden in production, is how a backend switch is driven from a client.
    environment("PROXY_DEV_SERVER_COMMAND", "true")
    standardInput = System.`in`
}

plugins {
    id("mapmaker.java-binary")
    id("mapmaker.packer-data")
    alias(libs.plugins.graalvm.native)
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":bin:config"))

    implementation(project(":modules:core"))
    implementation(project(":modules:datafix"))
    implementation(project(":modules:terraform-legacy"))
    implementation(project(":modules:map-runtime"))

    implementation(libs.minestom)
    implementation(libs.polar)
    implementation(libs.luau.core)
    implementation(libs.bundles.adventure)
    implementation(libs.slf4j.jul)
    implementation(libs.logback)
    implementation(libs.bundles.prometheus)
    implementation(libs.nats)
    implementation(libs.bundles.otel) {
        exclude(group = "io.opentelemetry", module = "opentelemetry-exporter-sender-okhttp")
    }

    nativeImageCompileOnly(project(":tools:native-image-helper"))
    configurations.named("nativeImageClasspath") {
//        exclude(group = "net.minestom", module = "data")
    }
}

val extractMinestomData = tasks.register<Copy>("extractMinestomData") {
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
    // The repository's zstd-jni entry is the JNI binding's. 1.5.7-22+ is a multi-release jar whose
    // 22+ classes call libzstd over FFM, so bin/config carries the metadata instead.
    metadataRepository {
        excludedModules.add("com.github.luben:zstd-jni")
        // These entries are for older versions and register (conditionally) methods that no longer exist,
        // which --exact-reachability-metadata refuses to parse. bin/config carries copies without those conditions.
        excludedModules.addAll(
            "ch.qos.logback:logback-classic",
            "io.opentelemetry:opentelemetry-sdk-metrics",
            "io.opentelemetry:opentelemetry-sdk-trace",
        )
    }

    binaries {
        named("main") {
            buildArgs(
                listOf(
                    "--enable-native-access=ALL-UNNAMED", "--enable-monitoring=jfr",
                    "--features=net.hollowcube.nativeimage.HCNativeImageFeature",
//                    "--gc=G1",

//                    "--future-defaults=all",
                    "-H:+UseCompressedReferences", "-R:MaxHeapSize=200m",
                    "--static-nolibc", "--no-fallback",
                    "--emit build-report",

                    // TODO: https enabled because we fetch skins from the session service. Should proxy (with cache)
                    //  this on the servers, or just store skins ourselves on player data
                    "--enable-url-protocols=http,https",

                    "--initialize-at-build-time=net.hollowcube.mapmaker.isolate.IsolateMain",

                    // Without exact metadata an unregistered lookup silently returns nothing (eg gson
                    // writing a record as {}). Warn logs each miss as a Missing*RegistrationError instead of
                    // throwing, which is what the CI smoke run fails on and what to search prod logs for.
                    "--exact-reachability-metadata",
                    "-R:MissingRegistrationReportingMode=Warn",

                    "--report-unsupported-elements-at-runtime",
                    "--initialize-at-build-time=it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap",
                    "--initialize-at-build-time=it.unimi.dsi.fastutil.ints.Int2ObjectMaps\$EmptyMap",
                    "--initialize-at-build-time=ch.qos.logback.classic.spi.LogbackServiceProvider",

                    "--initialize-at-build-time=net.hollowcube.mapmaker.map.runtime.MapServerInitializer",
                    "--initialize-at-build-time=net.hollowcube.datafix.DataFixer",
                    "--initialize-at-build-time=net.hollowcube.datafix",
                )
            )
        }
    }
}

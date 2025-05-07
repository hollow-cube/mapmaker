plugins {
    id("mapmaker.java-binary")
    id("mapmaker.packer-data")
    id("org.graalvm.buildtools.native") version "0.10.6"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":bin:config"))

    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-standalone"))
    implementation(project(":modules:core"))
    implementation(project(":modules:map"))
    implementation(project(":modules:terraform"))

    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.slf4j.jul)
    implementation(libs.logback)
    implementation(libs.bundles.prometheus)
    implementation(libs.bundles.otel)
}

application {
    mainClass = "net.hollowcube.mapmaker.map.IsolateMain"
}

graalvmNative {
    binaries {
        named("main") {
            fallback.set(false)
            buildArgs(listOf("--static-nolibc", "--no-fallback"))
        }
    }

    agent {
        enabled.set(true)

        metadataCopy {
            inputTaskNames.add("run")
            outputDirectories.add("resources/META-INF/native-image/net.hollowcube")
            mergeWithExisting.set(true)
        }
    }
}

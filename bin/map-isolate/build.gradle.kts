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

    nativeImageCompileOnly(project(":tools:native-image-helper"))
    configurations.named("nativeImageClasspath") {
        exclude(group = "net.minestom", module = "data")
    }
}

application {
    mainClass = "net.hollowcube.mapmaker.map.IsolateMain"
}

graalvmNative {
    binaries {
        named("main") {
            fallback.set(false)
            buildArgs(
                listOf(
                    "--enable-native-access=ALL-UNNAMED",
                    "--features=net.hollowcube.nativeimage.HCNativeImageFeature",
                    "--static-nolibc", "--no-fallback",
                    "--emit build-report",
                )
            )
        }
    }

//    agent {
//        enabled.set(true)
//
//        metadataCopy {
//            inputTaskNames.add("run")
//            outputDirectories.add("resources/META-INF/native-image/net.hollowcube")
//            mergeWithExisting.set(true)
//        }
//    }
}

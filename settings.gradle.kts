buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.google.code.gson:gson:2.11.0")
    }
}

plugins {
    // Settings plugins cannot be declared from the version catalog
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "mapmaker"

includeBuild("build-src")

// Submodule-d public dependencies
includeBuild("modules/molang")
includeBuild("modules/schem")

include(
    "modules:canvas:api",
    "modules:canvas:impl-standalone",
    "modules:command",
    "modules:common",
    "modules:compat",
    "modules:core",
    "modules:datafix",
    "modules:map-core",
    "modules:map-editor",
    "modules:map-runtime",
    "modules:nbs",
    "modules:replay",
    "modules:terraform",

    "modules:map-core-test",
)

include(
    "tools:native-image-helper",
    "tools:lua-slopgen:api",
    "tools:lua-slopgen",
)

include(
    "bin:config",
    "bin:development",
    "bin:example",
    "bin:hub",
    "bin:local",
    "bin:map",
    "bin:map-isolate",
    "bin:packer",
    "bin:proxy-plugin",
    "bin:world-converter",
)

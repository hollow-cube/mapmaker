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
    "modules:api",
    "modules:command",
    "modules:common",
    "modules:compat",
    "modules:core",
    "modules:datafix",
    "modules:ipc",
    "modules:map-core",
    "modules:map-editor",
    "modules:map-runtime",
    "modules:map-runtime-gen",
    "modules:map-runtime-gen:annotations",
    "modules:map-test",
    "modules:nbs",
    "modules:replay",
    "modules:terraform",

    "modules:test",
)

include(
    "tools:ipc-gen",
    "tools:native-image-helper",
    "tools:sql-gen",
    "tools:sql-gen:runtime",
    "tools:sql-gen:testing",
)

include(
    "bin:api-server",
    "bin:config",
    "bin:development",
    "bin:hub",
    "bin:map",
    "bin:map-isolate",
    "bin:packer",
    "bin:proxy-plugin",
    "bin:script-bundler",
    "bin:world-converter",
)

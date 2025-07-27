plugins {
    // Settings plugins cannot be declared from the version catalog
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
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
    "modules:hub",
    "modules:map",
    "modules:map-core",
    "modules:map-editor",
    "modules:map-runtime",
    "modules:nbs",
    "modules:replay",
    "modules:terraform",

    "modules:test",
    "modules:map-core-test",
)

include(
    "tools:native-image-helper",
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

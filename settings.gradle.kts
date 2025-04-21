plugins {
    // Settings plugins cannot be declared from the version catalog
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "mapmaker"

includeBuild("build-src")

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
    "modules:nbs",
    "modules:replay",
    "modules:script-engine",
    "modules:script-engine:builtin",
    "modules:script-engine:npm",
    "modules:terraform",
    "modules:test",
);

include(
    "bin:config",
    "bin:development",
    "bin:hub",
    "bin:local",
    "bin:map",
    "bin:packer",
    "bin:proxy-plugin",
)

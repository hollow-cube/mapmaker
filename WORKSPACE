load("@bazel_tools//tools/jdk:remote_java_repository.bzl", "remote_java_repository")

remote_java_repository(
    name = "zulu_20_macos_aarch64",
    prefix = "zulu",
    sha256 = "a2eff6a940c2df3a2352278027e83f5959f34dcfc8663034fe92be0f1b91ce6f",
    strip_prefix = "zulu20.28.85-ca-jdk20.0.0-macosx_aarch64",
    target_compatible_with = [
        "@platforms//cpu:aarch64",
        "@platforms//os:macos",
    ],
    url = "https://mirror.bazel.build/cdn.azul.com/zulu/bin/zulu20.28.85-ca-jdk20.0.0-macosx_aarch64.tar.gz",
    version = "20",
)

remote_java_repository(
    name = "zulu_20_macos_x64",
    prefix = "zulu",
    sha256 = "fde6cc17a194ea0d9b0c6c0cb6178199d8edfc282d649eec2c86a9796e843f86",
    strip_prefix = "zulu20.28.85-ca-jdk20.0.0-macosx_x64",
    target_compatible_with = [
        "@platforms//cpu:x86_64",
        "@platforms//os:macos",
    ],
    url = "https://mirror.bazel.build/cdn.azul.com/zulu/bin/zulu20.28.85-ca-jdk20.0.0-macosx_x64.tar.gz",
    version = "20",
)

remote_java_repository(
    name = "zulu_20_linux_x64",
    prefix = "zulu",
    sha256 = "0386418db7f23ae677d05045d30224094fc13423593ce9cd087d455069893bac",
    strip_prefix = "zulu20.28.85-ca-jdk20.0.0-linux_x64",
    target_compatible_with = [
        "@platforms//cpu:x86_64",
        "@platforms//os:linux",
    ],
    url = "https://mirror.bazel.build/cdn.azul.com/zulu/bin/zulu20.28.85-ca-jdk20.0.0-linux_x64.tar.gz",
    version = "20",
)

remote_java_repository(
    name = "zulu_20_windows_x64",
    prefix = "zulu",
    sha256 = "ac5f6a7d84dbbb0bb4d376feb331cc4c49a9920562f2a5e85b7a6b4863b10e1e",
    strip_prefix = "zulu20.28.85-ca-jdk20.0.0-win_x64",
    target_compatible_with = [
        "@platforms//cpu:x86_64",
        "@platforms//os:windows",
    ],
    url = "https://mirror.bazel.build/cdn.azul.com/zulu/bin/zulu20.28.85-ca-jdk20.0.0-win_x64.zip",
    version = "20",
)

register_toolchains("//:azul_jdk20_definition")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "4.5"

RULES_JVM_EXTERNAL_SHA = "b17d7388feb9bfa7f2fa09031b32707df529f26c91ab9e5d909eb1676badd9a6"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

rules_jvm_external_setup()

http_archive(
    name = "contrib_rules_jvm",
    sha256 = "4d62589dc6a55e74bbe33930b826d593367fc777449a410604b2ad7c6c625ef7",
    strip_prefix = "rules_jvm-0.19.0",
    url = "https://github.com/bazel-contrib/rules_jvm/releases/download/v0.19.0/rules_jvm-v0.19.0.tar.gz",
)

load("@contrib_rules_jvm//:repositories.bzl", "contrib_rules_jvm_deps")

contrib_rules_jvm_deps()

load("@contrib_rules_jvm//:setup.bzl", "contrib_rules_jvm_setup")

contrib_rules_jvm_setup()

load("//third_party/rules_jmh:defs.bzl", "rules_jmh_maven_deps")

rules_jmh_maven_deps()

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        # Quality
        "org.jetbrains:annotations:24.0.1",
        "com.google.auto.service:auto-service:1.1.1",
        "com.google.auto.service:auto-service-annotations:1.1.1",

        # Minestom
        "dev.hollowcube:minestom-ce-snapshots:1_20_2-a8a5242106",
        "dev.hollowcube:polar:1.4.0",
        "com.github.mworzala.mc_debug_renderer:minestom:74b86984b6",

        # Misc
        "com.google.code.gson:gson:2.10.1",
        "de.marhali:json5-java:2.0.0",
        "it.unimi.dsi:fastutil:8.5.12",
        "org.apache.kafka:kafka-clients:3.4.0",
        "org.spongepowered:configurate-core:4.1.2",
        "org.spongepowered:configurate-yaml:4.1.2",
        "com.miguelfonseca.completely:completely-core:0.9.0",
        "com.authzed.api:authzed:0.5.0",
        "io.grpc:grpc-protobuf:1.55.1",
        "io.grpc:grpc-stub:1.55.1",
        "com.google.protobuf:protobuf-java:3.24.4",
        "com.github.ben-manes.caffeine:caffeine:3.1.8",

        # Adventure
        "net.kyori:adventure-api:4.12.0",
        "net.kyori:adventure-key:4.12.0",
        "net.kyori:adventure-text-minimessage:4.12.0",
        "net.kyori:adventure-text-serializer-plain:4.12.0",

        # Helidon
        "io.helidon.health:helidon-health:3.0.2",
        "io.helidon.metrics:helidon-metrics-prometheus:3.0.2",
        "io.helidon.webserver:helidon-webserver:3.0.2",
        "io.helidon.logging:helidon-logging-slf4j:3.0.2",

        # Logging/Monitoring
        "org.slf4j:slf4j-api:2.0.9",
        "org.slf4j:jul-to-slf4j:2.0.6",
        "ch.qos.logback:logback-classic:1.4.5",
        "io.prometheus:simpleclient:0.16.0",
        "io.prometheus:simpleclient_hotspot:0.16.0",
        "io.pyroscope:agent:0.11.1",

        # Testing
        "org.junit.jupiter:junit-jupiter-api:5.10.0",
        "org.junit.jupiter:junit-jupiter-engine:5.10.0",
        "org.junit.jupiter:junit-jupiter-params:5.10.0",
        "org.junit.platform:junit-platform-suite-api:1.10.0",
        "org.junit.platform:junit-platform-launcher:1.10.0",
        "org.junit.platform:junit-platform-reporting:1.10.0",
        "org.junit.platform:junit-platform-suite-engine:1.10.0",

        # Compiler plugin util
        "org.burningwave:core:12.62.7",
    ],
    fetch_sources = True,
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://jitpack.io",
    ],
)

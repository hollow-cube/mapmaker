load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "5.3"

RULES_JVM_EXTERNAL_SHA = "d31e369b854322ca5098ea12c69d7175ded971435e55c18dd9dd5f29cc5249ac"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/releases/download/%s/rules_jvm_external-%s.tar.gz" % (RULES_JVM_EXTERNAL_TAG, RULES_JVM_EXTERNAL_TAG),
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

http_archive(
    name = "rules_graalvm",
    sha256 = "8faf2e1db03e8e370b67007197029a3b407306201dd83d20ca5a3e760a56288d",
    strip_prefix = "rules_graalvm-2c87605c9d65679f48d5b88082c6ccbd4785daf0",
    urls = [
        "https://github.com/sgammon/rules_graalvm/archive/2c87605c9d65679f48d5b88082c6ccbd4785daf0.zip",
    ],
)

load("@rules_graalvm//graalvm:repositories.bzl", "graalvm_repository")

graalvm_repository(
    name = "graalvm",
    distribution = "oracle",  # `oracle`, `ce`, or `community`
    java_version = "21",  # `17`, `20`, `22`, `23`, etc.
    version = "21.0.0",  # pass graalvm or specific jdk version supported by gvm
)

load("@rules_graalvm//graalvm:workspace.bzl", "register_graalvm_toolchains", "rules_graalvm_repositories")

rules_graalvm_repositories()

register_graalvm_toolchains()

http_archive(
    name = "rules_pkg",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_pkg/releases/download/1.0.1/rules_pkg-1.0.1.tar.gz",
        "https://github.com/bazelbuild/rules_pkg/releases/download/1.0.1/rules_pkg-1.0.1.tar.gz",
    ],
    sha256 = "d20c951960ed77cb7b341c2a59488534e494d5ad1d30c4818c736d57772a9fef",
)

load("@rules_pkg//:deps.bzl", "rules_pkg_dependencies")

rules_pkg_dependencies()

http_archive(
    name = "rules_oci",
    sha256 = "1bd16e455278d523f01326e0c3964cd64d7840a7e99cdd6e2617e59f698f3504",
    strip_prefix = "rules_oci-2.2.0",
    url = "https://github.com/bazel-contrib/rules_oci/releases/download/v2.2.0/rules_oci-v2.2.0.tar.gz",
)

load("@rules_oci//oci:dependencies.bzl", "rules_oci_dependencies")

rules_oci_dependencies()

load("@rules_oci//oci:repositories.bzl", "oci_register_toolchains")

oci_register_toolchains(name = "oci")

# You can pull your base images using oci_pull like this:
load("@rules_oci//oci:pull.bzl", "oci_pull")

oci_pull(
    name = "distroless_base",
    digest = "sha256:ccaef5ee2f1850270d453fdf700a5392534f8d1a8ca2acda391fbb6a06b81c86",
    image = "gcr.io/distroless/base",
    platforms = [
        "linux/amd64",
        "linux/arm64",
    ],
)

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//:specs.bzl", "maven")

maven_install(
    artifacts = [
        # Quality
        "org.jetbrains:annotations:24.0.1",
        "com.google.auto.service:auto-service:1.1.1",
        "com.google.auto.service:auto-service-annotations:1.1.1",

        # Minestom
        "net.minestom:minestom-snapshots:file_registries-4c14c81dba",
        "dev.hollowcube:polar:1.12.1",
        "dev.hollowcube:dataconverter:1.21.3-rv1",
        "dev.hollowcube:mql:1.1.0",

        # Misc
        "com.google.code.gson:gson:2.10.1",
        "de.marhali:json5-java:2.0.0",
        "it.unimi.dsi:fastutil:8.5.13",
        "org.apache.kafka:kafka-clients:3.4.0",
        "com.miguelfonseca.completely:completely-core:0.9.0",
        "com.github.ben-manes.caffeine:caffeine:3.1.8",
        "info.debatty:java-string-similarity:2.0.0",
        "com.velocitypowered:velocity-api:3.1.1",
        "org.jctools:jctools-core:4.0.5",

        # Adventure
        "net.kyori:adventure-api:4.16.0",
        "net.kyori:adventure-key:4.16.0",
        "net.kyori:adventure-text-minimessage:4.16.0",
        "net.kyori:adventure-text-serializer-plain:4.16.0",
        "net.kyori:adventure-nbt:4.16.0",

        # Logging/Monitoring
        "org.slf4j:slf4j-api:2.0.9",
        "org.slf4j:jul-to-slf4j:2.0.6",
        "ch.qos.logback:logback-classic:1.4.5",
        "io.prometheus:simpleclient:0.16.0",
        "io.prometheus:simpleclient_hotspot:0.16.0",
        "io.prometheus:simpleclient_httpserver:0.16.0",
        "io.opentelemetry:opentelemetry-api:1.45.0",
        "io.opentelemetry:opentelemetry-context:1.45.0",
        "io.opentelemetry:opentelemetry-sdk:1.45.0",
        "io.opentelemetry:opentelemetry-sdk-common:1.45.0",
        "io.opentelemetry:opentelemetry-sdk-trace:1.45.0",
        "io.opentelemetry:opentelemetry-extension-trace-propagators:1.45.0",
        "io.opentelemetry:opentelemetry-exporter-logging:1.45.0",
        "io.opentelemetry:opentelemetry-exporter-otlp:1.45.0",
        "io.opentelemetry:opentelemetry-exporter-sender-jdk:1.45.0",
        "io.opentelemetry.semconv:opentelemetry-semconv:1.28.0-alpha",
        "org.graalvm.sdk:nativeimage:24.1.1",
        "io.github.classgraph:classgraph:4.8.179",

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
    excluded_artifacts = [
        "com.velocitypowered:velocity-brigadier",  # Weird dependency issue, so just exclude it.
        "io.opentelemetry:opentelemetry-exporter-sender-okhttp",
        "com.github.hollow-cube:datafixerupper",  # Included by dataconverter but we replace it with our local version.
    ],
    fail_on_missing_checksum = False,
    fetch_javadoc = True,
    fetch_sources = True,
    repositories = [
        "m2Local",
        "https://repo1.maven.org/maven2",
        "https://jitpack.io",
        "https://repo.papermc.io/repository/maven-public/",
        "https://packages.confluent.io/maven/",  # For schema registry client
    ],
)

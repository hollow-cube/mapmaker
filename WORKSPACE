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

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//:specs.bzl", "maven")

maven_install(
    artifacts = [
        # Quality
        "org.jetbrains:annotations:24.0.1",
        "com.google.auto.service:auto-service:1.1.1",
        "com.google.auto.service:auto-service-annotations:1.1.1",

        # Minestom
        "net.minestom:minestom-snapshots:1_20_5-d51c6c75e2",
        "dev.hollowcube:polar:1.9.0",
        "dev.hollowcube:dataconverter:1.20.5-rv1",
        #        "com.github.mworzala.mc_debug_renderer:minestom:74b86984b6",

        # Misc
        "com.google.code.gson:gson:2.10.1",
        "de.marhali:json5-java:2.0.0",
        "it.unimi.dsi:fastutil:8.5.13",
        "org.apache.kafka:kafka-clients:3.4.0",
        "org.spongepowered:configurate-core:4.1.2",
        "org.spongepowered:configurate-yaml:4.1.2",
        "com.miguelfonseca.completely:completely-core:0.9.0",
        "info.debatty:java-string-similarity:2.0.0",
        "com.authzed.api:authzed:0.5.0",
        "io.grpc:grpc-protobuf:1.55.1",
        "io.grpc:grpc-stub:1.55.1",
        "com.google.protobuf:protobuf-java:3.24.4",
        "com.github.ben-manes.caffeine:caffeine:3.1.8",
        "io.getunleash:unleash-client-java:9.2.0",
        "com.velocitypowered:velocity-api:3.1.1",
        "com.google.inject:guice:7.0.0",
        "com.github.hollow-cube:datafixerupper:cf58e926a6",
        "org.apache.avro:avro:1.11.3",
        "io.confluent:kafka-schema-registry-client:7.6.0",

        # Adventure
        "net.kyori:adventure-api:4.16.0",
        "net.kyori:adventure-key:4.16.0",
        "net.kyori:adventure-text-minimessage:4.16.0",
        "net.kyori:adventure-text-serializer-plain:4.16.0",
        "net.kyori:adventure-nbt:4.16.0",

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
        "io.opentelemetry:opentelemetry-api:1.36.0",
        "io.opentelemetry:opentelemetry-context:1.36.0",
        "io.opentelemetry:opentelemetry-sdk:1.36.0",
        "io.opentelemetry:opentelemetry-sdk-common:1.36.0",
        "io.opentelemetry:opentelemetry-sdk-trace:1.36.0",
        "io.opentelemetry:opentelemetry-extension-trace-propagators:1.36.0",
        "io.opentelemetry:opentelemetry-exporter-logging:1.36.0",
        "io.opentelemetry:opentelemetry-exporter-otlp:1.36.0",
        "io.opentelemetry.semconv:opentelemetry-semconv:1.23.1-alpha",

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
    ],
    fail_on_missing_checksum = False,
    fetch_javadoc = True,
    fetch_sources = True,
    repositories = [
        #        "m2Local",
        "https://repo1.maven.org/maven2",
        "https://jitpack.io",
        "https://repo.papermc.io/repository/maven-public/",
        "https://packages.confluent.io/maven/",  # For schema registry client
    ],
)

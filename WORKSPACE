load("@bazel_tools//tools/jdk:remote_java_repository.bzl", "remote_java_repository")

remote_java_repository(
    name = "zulu_20_macos_aarch64",
    prefix = "zulu",
    sha256 = "a2eff6a940c2df3a2352278027e83f5959f34dcfc8663034fe92be0f1b91ce6f",
    strip_prefix = "zulu20.28.85-ca-jdk20.0.0-macosx_aarch64",
    target_compatible_with = [
        "@platforms//cpu:aarch64",  # x86_64
        "@platforms//os:macos",  # linux, windows
    ],
    url = "https://mirror.bazel.build/cdn.azul.com/zulu/bin/zulu20.28.85-ca-jdk20.0.0-macosx_aarch64.tar.gz",
    version = "20",
)

# todo add the rest of these
# https://mirror.bazel.build/cdn.azul.com/zulu/bin/zulu20.28.85-ca-jdk20.0.0-linux_aarch64.tar.gz
# https://mirror.bazel.build/cdn.azul.com/zulu/bin/zulu20.28.85-ca-jdk20.0.0-linux_x64.tar.gz
# https://mirror.bazel.build/cdn.azul.com/zulu/bin/zulu20.28.85-ca-jdk20.0.0-macosx_x64.tar.gz
# https://mirror.bazel.build/cdn.azul.com/zulu/bin/zulu20.28.85-ca-jdk20.0.0-win_x64.zip

register_toolchains("//:repository_default_toolchain_definition")

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

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "org.jetbrains:annotations:24.0.1",
        "org.slf4j:slf4j-api:2.0.9",
        "com.google.code.gson:gson:2.10.1",
        "com.google.auto.service:auto-service:1.1.1",
        "com.google.auto.service:auto-service-annotations:1.1.1",
        "dev.hollowcube:minestom-ce:e9d0098418",
        "dev.hollowcube:polar:1.3.1",
        "net.kyori:adventure-api:4.12.0",
        "net.kyori:adventure-key:4.12.0",
        "net.kyori:adventure-text-minimessage:4.12.0",
        "net.kyori:adventure-text-serializer-plain:4.12.0",
        "it.unimi.dsi:fastutil:8.5.12",

        # Compiler plugin util
        "org.burningwave:core:12.62.7",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://jitpack.io",
    ],
)

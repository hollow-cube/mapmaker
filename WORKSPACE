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

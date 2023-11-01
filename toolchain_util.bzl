load(
    "@bazel_tools//tools/jdk:default_java_toolchain.bzl",
    "BASE_JDK9_JVM_OPTS",
    "DEFAULT_JAVACOPTS",
    "DEFAULT_TOOLCHAIN_CONFIGURATION",
    "default_java_toolchain",
)

def add_zulu_toolchain(name, runtime):
    default_java_toolchain(
        name = name,
        configuration = DEFAULT_TOOLCHAIN_CONFIGURATION,
        java_runtime = runtime,
        javacopts = DEFAULT_JAVACOPTS + [
            "--enable-preview",
            "--add-modules",
            "jdk.incubator.concurrent",
        ],
        jvm_opts = BASE_JDK9_JVM_OPTS + ["--enable-preview"],
        source_version = "20",
        target_version = "20",
    )

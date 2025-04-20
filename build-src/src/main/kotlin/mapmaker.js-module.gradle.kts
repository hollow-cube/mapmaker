plugins {
    id("com.github.node-gradle.node")
}

node {
    download = true
    version = "22.14.0"
    workDir = file("${rootProject.projectDir}/.gradle/nodejs")
    npmWorkDir = file("${rootProject.projectDir}/.gradle/npm")
}

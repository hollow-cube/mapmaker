plugins {
    id("com.github.node-gradle.node") version "7.1.0"
}

node {
    download = true
    version = "22.14.0"
}

configurations.create("react")

artifacts {
    val nodeModules = layout.projectDirectory.dir("node_modules")
    for (artifact in listOf("react", "react-refresh", "react-reconciler", "scheduler")) {
        val artifactName = if (artifact == "react-refresh") "react-refresh-runtime" else artifact;
        val path = nodeModules.dir(artifact).dir("cjs").file("$artifactName.production.js")
        add("react", file(path)) {
            builtBy("npmInstall")
        }
    }
}

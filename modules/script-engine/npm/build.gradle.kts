plugins {
    id("mapmaker.js-module")
}

val isRelease = rootProject.properties.getOrDefault("isRelease", "false").toString().toBoolean()

configurations.create("react")

artifacts {
    val qualifier = if (isRelease) "production" else "development"

    val nodeModules = layout.projectDirectory.dir("node_modules")
    for (artifact in listOf("react", "react-refresh", "react-reconciler", "scheduler")) {
        val artifactName = if (artifact == "react-refresh") "react-refresh-runtime" else artifact;
        val path = nodeModules.dir(artifact).dir("cjs").file("$artifactName.$qualifier.js")
        add("react", file(path)) {
            builtBy("npmInstall")
        }
    }
}

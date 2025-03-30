import org.gradle.kotlin.dsl.dependencies

plugins {
    java
}

val packer by configurations.creating

dependencies {
    packer(project(":bin:packer", "packer"))
}

// Create a standard task that all consumers will have
tasks.register<Copy>("copyPackerData") {
    from(packer)
    into(layout.buildDirectory.dir("packer-data"))

    include("server/**")
    includeEmptyDirs = false
    eachFile {
        // The relativePath contains the full path including "server/"
        // We need to set the relativePath to remove this prefix
        relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
    }
}

// Configure all consumers to depend on the generated output
tasks.named("processResources") {
    dependsOn("copyPackerData")
}

java {
    sourceSets["main"].resources {
        srcDir(layout.buildDirectory.dir("packer-data"))
    }
}

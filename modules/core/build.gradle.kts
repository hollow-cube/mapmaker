plugins {
    id("mapmaker.java-library")
}

repositories {
    maven(url = "https://maven.noxcrew.com/public")
}

val builtin by configurations.creating

dependencies {
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:command"))
    implementation(project(":modules:compat"))
    implementation(project(":modules:common"))
    implementation(project(":modules:datafix"))

    implementation(libs.minestom)
    implementation(libs.bundles.otel)
    implementation(libs.bundles.adventure)
    implementation(libs.fastutil)
    implementation(libs.posthog)
    implementation(libs.polar)
    implementation(libs.similarity)
    implementation(libs.completely)
    implementation(libs.gson)
    implementation(libs.kafka)
    implementation(libs.jctools)
    implementation(libs.caffeine)
    implementation(libs.bundles.prometheus)

    testImplementation(project(":modules:test"))
//    builtin(project(":modules:script-engine:builtin", "builtin"))
}

//tasks.register<Copy>("copyBuiltinModule") {
//    from(builtin)
//    into(layout.buildDirectory.dir("builtin"))
//
//    eachFile {
//        val remapPath: List<String> = listOf("builtin") + relativePath.segments
//        relativePath = RelativePath(true, *remapPath.toTypedArray())
//    }
//}
//
//tasks.named("processTestResources") {
//    dependsOn("copyBuiltinModule")
//}
//
//java {
//    sourceSets["test"].resources {
//        srcDir(layout.buildDirectory.dir("builtin"))
//    }
//}

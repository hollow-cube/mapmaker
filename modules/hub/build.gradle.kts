dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:canvas:api"))
    runtimeOnly(project(":modules:canvas:impl-standalone"))

    val commonVersion = rootProject.property("commonVersion")
    implementation(project(":modules:instances"))
}
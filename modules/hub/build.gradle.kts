
dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-section"))
    runtimeOnly(project(":modules:canvas:impl-standalone"))

    val commonVersion = rootProject.property("commonVersion")
    implementation(project(":modules:instances"))
//    implementation("com.github.hollow-cube.common:instances:${commonVersion}")
}
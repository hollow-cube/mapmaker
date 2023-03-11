
dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-section"))

    val commonVersion = rootProject.property("commonVersion")
    implementation("com.github.hollow-cube.common:instances:${commonVersion}")
}
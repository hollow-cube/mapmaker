
dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:canvas"))

    val commonVersion = rootProject.property("commonVersion")
    implementation("com.github.hollow-cube.common:instances:${commonVersion}")
    implementation("com.github.hollow-cube.common:block-placement:${commonVersion}")

}


dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:canvas"))

    val commonVersion = rootProject.property("commonVersion")
    implementation("com.github.hollow-cube.common:instances:${commonVersion}")
}
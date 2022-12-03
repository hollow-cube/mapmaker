
dependencies {
    implementation(project(":modules:common"))

    val commonVersion = rootProject.property("commonVersion")
    implementation("com.github.hollow-cube.common:instances:${commonVersion}")
//    implementation("com.github.hollow-cube.common:block-placement:${commonVersion}")

}

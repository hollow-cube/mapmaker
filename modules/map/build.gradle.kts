
dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:canvas:api"))
    implementation(project(":modules:canvas:impl-section"))
    implementation(project(":modules:terraform"))

    val commonVersion = rootProject.property("commonVersion")
    implementation(project(":modules:instances"))
//    implementation("com.github.hollow-cube.common:instances:${commonVersion}")
    implementation("com.github.hollow-cube.common:block-placement:${commonVersion}")

}

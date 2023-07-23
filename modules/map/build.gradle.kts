dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:replay"))
    implementation(project(":modules:canvas:api"))
    runtimeOnly(project(":modules:canvas:impl-standalone"))
    implementation(project(":modules:terraform"))

    val commonVersion = rootProject.property("commonVersion")
    implementation("com.github.hollow-cube.common:block-placement:${commonVersion}")

    implementation("com.github.mworzala.mc_debug_renderer:minestom:2c354a8e0859b765144d7c629c2a4d62b5f1d220")


    implementation(libs.polar)
}

dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:canvas:api"))
    runtimeOnly(project(":modules:canvas:impl-standalone"))

    implementation(libs.polar)

    implementation("com.miguelfonseca.completely:completely-core:0.9.0")
}
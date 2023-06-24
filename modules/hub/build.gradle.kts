dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:canvas:api"))
    runtimeOnly(project(":modules:canvas:impl-standalone"))

    implementation("com.amazonaws:aws-java-sdk-s3:1.12.429")

    implementation("com.miguelfonseca.completely:completely-core:0.9.0")
}
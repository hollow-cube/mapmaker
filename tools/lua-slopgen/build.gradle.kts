plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(project(":tools:lua-slopgen:api"))

    implementation(libs.javapoet)
    implementation(libs.luau.lib)
}

plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:command"))
    implementation(project(":modules:core")) // TODO: This is a terrible dependency, but need the action bar stuff for debug stick.
    implementation(project(":modules:compat"))
    implementation(libs.dataconverter) {
        exclude(group = "com.github.hollow-cube")
    }
    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.gson)
    implementation(libs.fastutil)
}

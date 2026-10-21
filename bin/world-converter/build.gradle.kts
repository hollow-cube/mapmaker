plugins {
    id("mapmaker.java-binary")
}

dependencies {
    implementation(libs.logback)
    implementation(libs.minestom)
    implementation(libs.polar)
    implementation(libs.fastutil)

    implementation(project(":modules:datafix"))
    implementation(project(":modules:map-core"))
    implementation(project(":modules:terraform-legacy"))
}

application {
    mainClass.set("net.hollowcube.worldconverter.Main")
}

plugins {
    id("mapmaker.java-binary")
}

dependencies {
    implementation(libs.logback)
    implementation(libs.minestom)
    implementation(libs.polar)

    implementation(project(":modules:map-core"))
}

application {
    mainClass.set("net.hollowcube.worldconverter.Main")
}
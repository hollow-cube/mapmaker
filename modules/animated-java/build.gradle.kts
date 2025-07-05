plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.logback)

    implementation("net.hollowcube:multipart:-INCLUDED")
}

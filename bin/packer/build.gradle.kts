plugins {
    application
}

dependencies {
    // We will use json-java to remove comments from JSON, but moshi for the actual parsing
    implementation("org.json:json:20230227")
    implementation("com.squareup.moshi:moshi:1.15.0")

    implementation("org.jetbrains:annotations:24.0.1")


}

application {
    mainClass.set("net.hollowcube.mapmaker.Packer")
}

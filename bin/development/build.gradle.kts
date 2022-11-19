plugins {
    application
}

dependencies {
    implementation("com.github.minestommmo:Minestom:${rootProject.property("minestomVersion")}")
}

application {
    mainClass.set("net.hollowcube.server.StartServer")
}

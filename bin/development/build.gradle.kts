plugins {
    application
}

dependencies {
    implementation("com.github.minestommmo:Minestom:${rootProject.property("minestomVersion")}")
    implementation("com.github.hollow-cube.common:common:f73dc3434ef99")
}

application {
    mainClass.set("net.hollowcube.mapmaker.dev.Main")
}

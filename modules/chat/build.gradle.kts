plugins {
    `java-library`
}

dependencies {
    implementation(project(":modules:core")) // Not sure i am a fan of this dependency

    compileOnly("org.mongodb:mongodb-driver-sync:4.7.0")
    testImplementation("org.mongodb:mongodb-driver-sync:4.7.0")
}

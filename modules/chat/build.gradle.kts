plugins {
    `java-library`
}

dependencies {
    implementation(project(":modules:common")) // Not sure i am a fan of this dependency
    compileOnly("org.mongodb:mongodb-driver-sync:4.7.0")
}

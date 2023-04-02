
dependencies {
    implementation(project(":modules:canvas:api"))
    testImplementation(project(":modules:canvas:api"))

    testImplementation("ch.qos.logback:logback-classic:1.4.5")
    testImplementation("org.slf4j:jul-to-slf4j:2.0.6")

//    implementation(files("/Users/matt/HeadDatabase-4.17.0.jar"))
}

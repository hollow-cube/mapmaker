plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(project(":tools:lua-slopgen:api"))

    implementation(libs.javapoet)
    implementation(libs.luau.core)
    implementation(libs.fastutil)

    testImplementation(libs.compile.testing)
}

tasks.test {
    // Forward the golden-update flag to the test JVM so callers can refresh expected files.
    System.getProperty("slopgen.update_goldens")?.let {
        systemProperty("slopgen.update_goldens", it)
    }
}

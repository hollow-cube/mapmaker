plugins {
    id("mapmaker.java-library")
}

dependencies {
    // api so callers see Model record component types (LuaLibrary.Scope, JavaPoet TypeName, etc.).
    api(project(":modules:map-runtime-gen:annotations"))
    api(libs.javapoet)

    implementation(libs.luau.core)
    implementation(libs.fastutil)
    implementation(libs.gson)

    testImplementation(libs.compile.testing)
}

tasks.test {
    // Forward the golden-update flag to the test JVM so callers can refresh expected files.
    System.getProperty("slopgen.update_goldens")?.let {
        systemProperty("slopgen.update_goldens", it)
    }
}

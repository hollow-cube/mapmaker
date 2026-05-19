plugins {
    id("mapmaker.java-library")
}

dependencies {
    // api so consumers (e.g. :tools:lua-slopgen:engine-api) get LuaLibrary.Scope etc., which
    // appear in `Model.Library` record components.
    api(project(":tools:lua-slopgen:api"))
    // javapoet TypeName/ClassName also appear in record components (Model.Export.javaType etc.).
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

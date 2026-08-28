plugins {
    id("mapmaker.java-library")
}

dependencies {
    api(project(":tools:sql-gen:runtime"))
    api(libs.junit.api)

    // TestDb boots the same in-process Postgres the generator analyses against.
    implementation(libs.pglite4j)
}

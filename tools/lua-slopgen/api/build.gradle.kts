plugins {
    id("mapmaker.java-library")
}

dependencies {
    // GeneratedStringAtoms contains a fastutil map, so users must have it.
    api(libs.fastutil)

    api(libs.luau.core)
}

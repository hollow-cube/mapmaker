plugins {
    id("mapmaker.java-library")
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.bundles.adventure)
    implementation(libs.gson)
    implementation(libs.caffeine)
    implementation(libs.posthog)
    implementation(libs.fastutil)
    implementation(libs.included.schem)

    // `:modules:test` carries the packer output, which is where `BadSprite` reads `sprites.json`
    // from. Without it the sprite map is empty and every `BadSprite.require` in a test NPEs.
    testImplementation(project(":modules:test"))
}

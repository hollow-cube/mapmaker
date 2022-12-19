plugins {
    id("net.ltgt.errorprone") version "2.0.2" apply false
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
}

allprojects {
    tasks.withType<Copy> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

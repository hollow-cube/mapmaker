plugins {
    id("mapmaker.java-library")
}

// Intentionally no runtime dependencies. The annotations are CLASS-retention and consumed
// `compileOnly` by map-runtime — nothing here should leak onto map-runtime's runtime classpath.

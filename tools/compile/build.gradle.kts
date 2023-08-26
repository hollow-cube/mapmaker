plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.burningwave:core:12.62.7")
}

val exports = listOf(
        "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED"
)

tasks {
    compileJava {
        options.compilerArgs.addAll(listOf("--add-modules", "java.compiler") + exports)
    }

    compileTestJava {
        options.compilerArgs.addAll(listOf("--add-modules", "java.compiler") + exports)
    }

    test {
        jvmArgs("--add-modules", "java.compiler")
        jvmArgs(*exports.toTypedArray())
    }
}

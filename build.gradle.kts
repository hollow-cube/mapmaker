// Plugins that more than one subproject applies are declared here (unapplied) so that they load in
// the root class loader and every subproject shares the one copy. Applied separately from two build
// scripts, the GraalVM plugin ends up loaded twice and its build service fails to cast to itself as
// soon as both projects are configured in one build.
plugins {
    alias(libs.plugins.graalvm.native) apply false
    alias(libs.plugins.javafmt) apply false
}

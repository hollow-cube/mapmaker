rootProject.name = "mapmaker"

include(":modules")
// Standalone modules
include(":modules:common")
include(":modules:canvas")
include(":modules:terraform")
include(":modules:chat")

// Mapmaker modules
include(":modules:core")
include(":modules:hub")
include(":modules:map")

// Binaries
include(":bin")
include(":bin:development")

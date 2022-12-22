rootProject.name = "mapmaker"

include(":modules")
// Standalone modules
include(":modules:canvas")
include(":modules:terraform")
include(":modules:chat")

// Mapmaker modules
include(":modules:common")
include(":modules:hub")
include(":modules:map")

// Binaries
include(":bin")
include(":bin:development")

rootProject.name = "mapmaker"

include(":modules")
// Standalone modules
include(":modules:common")
include(":modules:terraform")
include(":modules:chat")
include(":modules:canvas")
include(":modules:canvas:api")
include(":modules:instances")
//include(":modules:canvas:impl-standalone")
include(":modules:canvas:impl-section")

// Mapmaker modules
include(":modules:core")
include(":modules:hub")
include(":modules:map")

// Binaries
include(":bin")
include(":bin:development")

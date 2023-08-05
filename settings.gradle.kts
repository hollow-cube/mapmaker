rootProject.name = "mapmaker"

// Tools
include(":tools")
include(":tools:compile")

include(":modules")
// Standalone modules
include(":modules:common")
include(":modules:terraform")
include(":modules:replay")
include(":modules:chat")
include(":modules:canvas")
include(":modules:canvas:api")
include(":modules:canvas:impl-standalone")
//include(":modules:canvas:impl-mock")

// Mapmaker modules
include(":modules:core")
include(":modules:hub")
include(":modules:map")

// Binaries
include(":bin")
include(":bin:packer")
include(":bin:development")

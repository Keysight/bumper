rootProject.name = "dobby-all"

include(":shell-parser")
project(":shell-parser").projectDir = file("shell-parser")

include(":dobby")
project(":dobby").projectDir = file("dobby")

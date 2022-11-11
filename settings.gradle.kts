rootProject.name = "bumper"

include(":bumper-core")
project(":bumper-core").projectDir = file("core/")

include(":bumper-libclang")
project(":bumper-libclang").projectDir = file("implementations/libclang")

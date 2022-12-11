pluginManagement {
    repositories {
        // We first try the nexus proxy for gradle plugins (visible from bamboo)
        maven {
            url = uri("http://nexus3.riscure.com:8081/repository/gradle-central-plugins/")
            isAllowInsecureProtocol = true
        }
        // If it fails, we default on gradle plugin portal (not visible from bamboo, requires internet)
        gradlePluginPortal()
    }
}

rootProject.name = "bumper"

include(":bumper-core")
project(":bumper-core").projectDir = file("core/")

include(":bumper-test")
project(":bumper-test").projectDir = file("test/")

include(":bumper-libclang")
project(":bumper-libclang").projectDir = file("implementations/libclang")

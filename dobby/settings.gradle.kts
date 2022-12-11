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

rootProject.name = "dobby-all"

include(":shell-parser")
project(":shell-parser").projectDir = file("shell-parser")

include(":dobby")
project(":dobby").projectDir = file("dobby")

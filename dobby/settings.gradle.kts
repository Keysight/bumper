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

val antlr = "4.11.1"

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("http://nexus3.riscure.com:8081/repository/maven-central/")
            isAllowInsecureProtocol = true
        }
    }

    versionCatalogs {
        create("kotlinx") {
            library("json", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
        }
        create("libs") {
            library("arrow.core", "io.arrow-kt:arrow-core:1.1.2")
            library("antlr.runtime", "org.antlr:antlr4-runtime:$antlr")
            library("antlr.generator", "org.antlr:antlr4:$antlr")
            library("apache.commons", "org.apache.commons:commons-lang3:3.8.1")
        }
    }
}

rootProject.name = "dobby-all"

include(":shell-parser")
project(":shell-parser").projectDir = file("shell-parser")

include(":dobby")
project(":dobby").projectDir = file("dobby")

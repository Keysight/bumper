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

    dependencyResolutionManagement {
        repositories {
            maven {
                url = uri("http://nexus3.riscure.com:8081/repository/riscure")
                isAllowInsecureProtocol = true
            }

            maven {
                url = uri("http://nexus3.riscure.com:8081/repository/riscure-snapshots")
                isAllowInsecureProtocol = true
            }

            maven {
                url = uri("http://nexus3.riscure.com:8081/repository/maven-central/")
                isAllowInsecureProtocol = true
            }

            maven {
                url = uri("http://nexus3.riscure.com:8081/repository/3rdparty")
                isAllowInsecureProtocol = true
            }

            maven {
                url = uri("http://nexus3.riscure.com:8081/repository/3rdparty-snapshots")
                isAllowInsecureProtocol = true
            }
        }

        val serialization = "1.5.1"
        val coroutines = "1.7.3"

        versionCatalogs {
            create("kotlinx") {
                library("coroutines-core"        , "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
                library("serialization-core"     , "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$serialization")
                library("serialization-protobuf" , "org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serialization")
                library("serialization-cbor"     , "org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialization")
                library("serialization-json"     , "org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization")
            }

            create("libs") {
                library("arrow.core"     , "io.arrow-kt:arrow-core:1.1.2")
                library("apache.lang3"   , "org.apache.commons:commons-lang3:3.8.1")
                library("slf4j"          , "org.slf4j:slf4j-api:1.7.25")
                library("process"        , "com.github.pgreze:kotlin-process:1.4")
                library("junit"          , "org.junit.jupiter:junit-jupiter:5.8.2")
                library("dobby"          , "com.riscure:riscure-dobby:0.1.9-alpha-5")
                library("bytedeco"       , "org.bytedeco:llvm-platform:16.0.4-1.5.9")
                library("picocli"        , "info.picocli:picocli:4.6.3")
                library("antlr"          , "org.antlr:antlr4:4.11.1")
                library("jflex"          , "de.jflex:jflex:1.9.1")
                library("mordant"        , "com.github.ajalt.mordant:mordant:2.1.0")
            }
        }
    }
}

rootProject.name = "bumper"

include(":bumper-core")
project(":bumper-core").projectDir = file("core/")

include(":bumper-test")
project(":bumper-test").projectDir = file("test/")

include(":bumper-libclang")
project(":bumper-libclang").projectDir = file("implementations/libclang")

include(":bumper-highlight")
project(":bumper-highlight").projectDir = file("highlight/")

val localSettings = file("local.settings.gradle.kts")
if (localSettings.exists()) {
    apply(from = localSettings)
}


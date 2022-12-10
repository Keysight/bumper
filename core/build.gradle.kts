import org.gradle.api.tasks.testing.logging.TestExceptionFormat.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

description = "Riscure True Code C Frontend"

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("org.jetbrains.dokka") version "1.7.20"

    `maven-publish`
}

group   = "com.riscure"
version = "0.1.0-SNAPSHOT"

val releases  = uri("http://nexus3.riscure.com:8081/repository/riscure")
val snapshots = uri("http://nexus3.riscure.com:8081/repository/riscure-snapshots")

repositories {
    maven { url = releases ; isAllowInsecureProtocol = true }
    maven { url = snapshots; isAllowInsecureProtocol = true }

    // Maven central is proxied through nexus
    maven {
        url = uri("http://nexus3.riscure.com:8081/repository/maven-central/")
        isAllowInsecureProtocol = true
    }

    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.4.1")

    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("io.arrow-kt:arrow-core:1.1.2")
    implementation("com.github.pgreze:kotlin-process:1.4")
    implementation("com.riscure:riscure-dobby:0.1.0-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation(kotlin("test"))
}

tasks.named<Test>("test") {
  useJUnitPlatform()
  testLogging {
    events(PASSED, FAILED, STANDARD_OUT, STANDARD_ERROR, SKIPPED)
    exceptionFormat = FULL
  }
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers" + "-Xskip-prerelease-check"
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers" + "-Xskip-prerelease-check"
    }
}

// Publishing
fun systemProperty(key: String): String? = System.getProperty(key)

val bambooLocal = systemProperty("bambooMavenLocalRepo") ?: "../../../.repo/"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId    = "com.riscure"
            artifactId = "riscure-bumper-core"
            version    = version

            from(components["java"])

            pom {
                name.set(rootProject.name)
                description.set("A C frontend")
            }
        }
    }

    repositories {
        maven {
            url = uri(bambooLocal)
            name = "localBamboo"
            isAllowInsecureProtocol = true
        }
    }
}

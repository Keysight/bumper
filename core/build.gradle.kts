import org.gradle.api.tasks.testing.logging.TestExceptionFormat.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

description = "Riscure True Code C Frontend"

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("org.jetbrains.dokka")

    `maven-publish`
}

dependencies {
    implementation(kotlinx.coroutines.core)
    implementation(kotlinx.serialization.core)
    implementation(kotlinx.serialization.cbor)

    implementation(libs.slf4j)
    implementation(libs.arrow.core)
    implementation(libs.process)
    implementation(libs.dobby)

    testImplementation(libs.junit)
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
        freeCompilerArgs = freeCompilerArgs +
                "-Xjvm-default=all" +
                "-Xskip-prerelease-check"
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs +
                "-Xjvm-default=all" +
                "-xskip-prerelease-check"
    }
}

fun env(key: String): String? = System.getenv(key)

val nexusUsername = env("NEXUS_USERNAME")
val nexusPassword = env("NEXUS_PASSWORD")

val releases  = uri("http://nexus3.riscure.com:8081/repository/riscure")
val snapshots = uri("http://nexus3.riscure.com:8081/repository/riscure-snapshots")

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
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshots else releases)
            isAllowInsecureProtocol = true
            credentials {
                username = nexusUsername
                password = nexusPassword
            }
        }
    }
}

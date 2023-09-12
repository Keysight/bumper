import org.gradle.api.tasks.testing.logging.TestExceptionFormat.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

description = "Riscure True Code C Frontend -- Libclang Implementation"

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")

    `maven-publish`
    application

    id("org.bytedeco.gradle-javacpp-platform") version "1.5.9"
}

// supported native platforms
val javacppPlatform by extra {
    "linux-x86_64,windows-x86_64"
}

application {
    mainClass.set("com.riscure.bumper.BumperCmdKt")
}

dependencies {
    implementation(kotlinx.coroutines.core)

    api(libs.bytedeco)
    implementation(libs.process)
    implementation(libs.arrow.core)
    implementation(libs.dobby)
    implementation(libs.picocli)
    implementation(project(":bumper-core"))

    testImplementation(libs.junit)
    testImplementation(project(":bumper-test"))
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
        freeCompilerArgs = freeCompilerArgs
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs
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
            artifactId = "riscure-bumper-libclang"
            version    = version

            from(components["java"])

            pom {
                name.set(rootProject.name)
                description.set("Bumper libclang implementation")
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

import org.gradle.api.tasks.testing.logging.TestExceptionFormat.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

description = "Riscure True Code C Frontend"

plugins {
    kotlin("jvm") version "1.7.10"
}

group = "com.riscure"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // FIXME
    // This comes with a lot of llvms.
    // Figure out what gets shipped, and whether we should use a different version.
    implementation("org.bytedeco:llvm-platform:11.1.0-1.5.5")

    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("io.arrow-kt:arrow-core:1.0.1")

    implementation(project(":dobby"))

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
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xcontext-receivers"
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xcontext-receivers"
    }
}

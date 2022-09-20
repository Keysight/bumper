import org.gradle.api.tasks.testing.logging.TestExceptionFormat.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

description = "Riscure True Code C Parser"

plugins {
    id("com.riscure.java-conventions")
    id("com.riscure.llvm")

    // is this real life
    kotlin("jvm") version "1.7.10"
}

dependencies {
    // TODO
    // Is this correct? core uses a SNAPSHOT?!
    implementation("org.bytedeco:llvm-platform:11.0.0-1.5.5-SNAPSHOT")

    implementation(project(":tc-core"))
    implementation(project(":tc-codeanalysis"))

    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("io.arrow-kt:arrow-core:1.0.1")

    testRuntimeOnly(files(tasks.getByName("unpackClang")))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.named<Test>("test") {
  useJUnitPlatform()
  testLogging {
    events(PASSED, FAILED, STANDARD_OUT, STANDARD_ERROR, SKIPPED)
    exceptionFormat = FULL
  }
}

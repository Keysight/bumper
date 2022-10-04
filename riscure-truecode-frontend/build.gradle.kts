import org.gradle.api.tasks.testing.logging.TestExceptionFormat.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

description = "Riscure True Code C Frontend"

plugins {
    id("com.riscure.java-conventions")
    kotlin("jvm") version "1.7.10"
}

dependencies {
    // FIXME
    implementation("org.bytedeco:llvm-platform:11.0.0-1.5.5-SNAPSHOT")

    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("io.arrow-kt:arrow-core:1.0.1")
    implementation("com.github.pgreze:kotlin-process:1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")

    implementation(project(":tc-codeanalysis"))
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

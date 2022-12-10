import org.gradle.api.tasks.testing.logging.TestExceptionFormat.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

description = "Riscure True Code C Frontend -- Libclang Implementation"

plugins {
    kotlin("jvm") version "1.7.20"
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

    // "llvm-platform" is located in "3rdparty-snapshots" repo in nexus
    maven  {
        url = uri("http://nexus3.riscure.com:8081/repository/3rdparty-snapshots/")
        isAllowInsecureProtocol = true
    }

    mavenLocal()
}

dependencies {
    implementation("org.bytedeco:llvm-platform:11.0.0-1.5.5-SNAPSHOT")

    implementation("io.arrow-kt:arrow-core:1.1.2")
    implementation("com.github.pgreze:kotlin-process:1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")

    implementation("com.riscure:riscure-dobby:0.1.0-SNAPSHOT")
    implementation(project(":bumper-core"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
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
            url = uri(bambooLocal)
            name = "localBamboo"
            isAllowInsecureProtocol = true
        }
    }
}

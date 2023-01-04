import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    `maven-publish`

    kotlin("jvm")
    kotlin("plugin.serialization")
}

fun systemProperty(key: String): String? = System.getProperty(key)

val bambooLocal = systemProperty("bambooMavenLocalRepo") ?: "../../../.repo/"

group   = "com.riscure"
version = "0.1.0-SNAPSHOT"

repositories {
    maven {
        url = uri(bambooLocal)
        name = "localBamboo"
        isAllowInsecureProtocol = true
    }

    maven {
        url = uri("http://nexus3.riscure.com:8081/repository/maven-central/")
        isAllowInsecureProtocol = true
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation("io.arrow-kt:arrow-core:1.1.2")
    implementation("org.antlr:antlr4-runtime:4.11.1")

    implementation(project(":shell-parser"))

    implementation("org.apache.commons:commons-lang3:3.8.1")

    // resources
    runtimeOnly(files("./src/main/resources/clang.options.json"))

    // test deps
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events(PASSED, FAILED, STANDARD_OUT, STANDARD_ERROR, SKIPPED)
        exceptionFormat = FULL
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf()
    }
}

// Publishing
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId    = "com.riscure"
            artifactId = "riscure-dobby"
            version    = version

            from(components["java"])

            pom {
                name.set(rootProject.name)
                description.set("The friendly compilation database elf")
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

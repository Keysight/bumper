import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    `maven-publish`

    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlinx.json)

    implementation(libs.arrow.core)
    implementation(libs.antlr.runtime)
    implementation(libs.apache.commons)

    implementation(project(":shell-parser"))

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
}

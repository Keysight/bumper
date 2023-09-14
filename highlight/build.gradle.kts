import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

description = "Riscure C Syntax Highlighter"

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")

    `maven-publish`
    application
    antlr
}

application {
    mainClass.set("com.riscure.bumper.highlight.RenderKt")
}

val jflex = configurations.create("jflex")
val jflexGen = project.layout.buildDirectory.dir("jflex")

/** jflex lexer generation */

val generateLexer = tasks.register("generateLexer") {
    inputs.dir("./src/main/jflex/")
    outputs.dir(jflexGen)

    doLast {
        // JFlex has no gradle plugin, but they do have an ant plugin.
        // Because Gradle integrates with Ant, we can configure that one.
        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "jflex",
                "classname" to "jflex.anttask.JFlexTask",
                "classpath" to jflex.asPath
            )

            "jflex"(
                "file" to file("./src/main/jflex/c.jflex"),
                "destdir" to jflexGen.get().asFile.toString()
            )
        }
    }
}

sourceSets["main"].java {
    srcDir(jflexGen)
}

dependencies {
    implementation(project(":bumper-core"))

    implementation(kotlinx.coroutines.core)
    implementation(kotlinx.serialization.core)
    implementation(kotlinx.serialization.json)

    implementation(libs.arrow.core)
    implementation(libs.mordant)

    implementation(project.files(generateLexer))

    jflex(libs.jflex)

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
                "-Xjvm-default=all"
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs +
                "-Xjvm-default=all"
    }
}

/** publishing */

fun env(key: String): String? = System.getenv(key)

val nexusUsername = env("NEXUS_USERNAME")
val nexusPassword = env("NEXUS_PASSWORD")

val releases  = uri("http://nexus3.riscure.com:8081/repository/riscure")
val snapshots = uri("http://nexus3.riscure.com:8081/repository/riscure-snapshots")

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId    = "com.riscure"
            artifactId = "riscure-bumper-highlight"
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

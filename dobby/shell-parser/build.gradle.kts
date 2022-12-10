plugins {
    `maven-publish`
    antlr
}

group   = "com.riscure"
version = "0.1.0-SNAPSHOT"

repositories {
    maven {
        url = uri("http://nexus3.riscure.com:8081/repository/maven-central/")
        isAllowInsecureProtocol = true
    }
}

dependencies {
    // Use Antlr 4 for the parser generation
    antlr("org.antlr:antlr4:4.11.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf()
    }
}

// ANTLR plugin configuration.
// This plugin really behaves meh.
tasks.generateGrammarSource {
    arguments   = arguments + listOf(
        "-lib", "./src/main/antlr/com/riscure/lang/shell/",
        "-no-visitor",
        "-no-listener"
    )
}

// Publishing
fun systemProperty(key: String): String? = System.getProperty(key)

val bambooLocal = systemProperty("bambooMavenLocalRepo") ?: "../../../.repo/"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId    = "com.riscure"
            artifactId = "riscure-shell-parser"
            version    = version

            from(components["java"])

            pom {
                name.set(rootProject.name)
                description.set("Riscure' shell parser")
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
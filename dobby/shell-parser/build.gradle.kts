plugins {
    `maven-publish`
    antlr
}

dependencies {
    // Use Antlr 4 for the parser generation
    antlr(libs.antlr.generator)
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

fun env(key: String): String? = System.getenv(key)

val nexusUsername = env("NEXUS_USERNAME")
val nexusPassword = env("NEXUS_PASSWORD")

val releases  = uri("http://nexus3.riscure.com:8081/repository/riscure")
val snapshots = uri("http://nexus3.riscure.com:8081/repository/riscure-snapshots")

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
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshots else releases)
            isAllowInsecureProtocol = true
            credentials {
                username = nexusUsername
                password = nexusPassword
            }
        }
    }
}

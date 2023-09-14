plugins {
    `maven-publish`
    antlr
}

group   = "com.riscure"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Use Antlr 4 for the parser generation
    antlr("org.antlr:antlr4:4.11.1")
}

// ANTLR plugin configuration.
// This plugin really behaves meh.
tasks.generateGrammarSource {
    arguments   = arguments + listOf(
        "-lib", "./src/main/antlr/com/riscure/bumper/antlr/",
        "-no-visitor",
        "-no-listener"
    )
}

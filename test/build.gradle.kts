description = "Riscure True Code C Frontend Test Fixtures"

plugins {
    kotlin("jvm") version "1.7.20"
}

group   = "com.riscure"
version = "0.1.0-SNAPSHOT"

val releases  = uri("http://nexus3.riscure.com:8081/repository/riscure")
val snapshots = uri("http://nexus3.riscure.com:8081/repository/riscure-snapshots")

repositories {
    maven { url = releases ; isAllowInsecureProtocol = true }
    maven { url = snapshots; isAllowInsecureProtocol = true }

    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":bumper-core"))
    implementation("com.riscure:riscure-dobby:0.1.0-SNAPSHOT")
    implementation("io.arrow-kt:arrow-core:1.1.2")
    implementation("org.junit.jupiter:junit-jupiter:5.8.2")
    implementation(kotlin("test"))
    implementation(files("./src/main/resources"))
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

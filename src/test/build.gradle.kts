description = "Riscure True Code C Frontend Test Fixtures"

plugins {
    kotlin("jvm")
}

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

    mavenLocal()
}

dependencies {
    implementation(project(":bumper-core"))
    implementation(project(":dobby"))
    implementation(libs.arrow.core)
    implementation(kotlinx.serialization.json)
    implementation(libs.junit)
    implementation(kotlin("test"))
    implementation(files("./src/main/resources"))
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs
    }
}

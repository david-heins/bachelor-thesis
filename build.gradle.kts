import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    application
    alias(libs.plugins.jvm)
    alias(libs.plugins.ksp)
}

group = "de.telekom.davidheins"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.arrow.optics)
    implementation(libs.clikt)
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.rabbitmq.amqp)
    implementation(libs.rabbitmq.stream)
    implementation(libs.slf4j)
    ksp(libs.arrow.optics.ksp.plugin)
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(17)
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(false)
            apiVersion.set(KotlinVersion.KOTLIN_1_9)
            languageVersion.set(KotlinVersion.KOTLIN_1_9)
            if (project.findProperty("debug")?.toString()?.toBooleanStrictOrNull() == true) {
                freeCompilerArgs.add("-Xdebug")
            }
        }
    }
}

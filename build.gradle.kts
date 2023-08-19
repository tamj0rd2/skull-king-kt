import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    application
}

group = "com.tamj0rd2"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.http4k:http4k-server-jetty:5.6.3.0")
    implementation("org.http4k:http4k-core:5.6.3.0")
    implementation("org.http4k:http4k-template-handlebars:5.6.3.0")
    implementation("org.http4k:http4k-format-jackson:5.6.3.0")

    testImplementation(kotlin("test"))
    testImplementation("org.seleniumhq.selenium:selenium-chrome-driver:4.11.0")
    testImplementation("com.natpryce:hamkrest:1.8.0.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}
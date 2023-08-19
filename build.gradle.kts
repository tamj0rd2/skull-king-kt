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
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:kotlin-stdlib-jdk8${kotlinVersion}")
    implementation(platform("org.http4k:http4k-bom:5.6.3.0"))
    implementation("org.http4k:http4k-server-jetty:5.6.3.0")
    implementation("org.http4k:http4k-core:5.6.3.0")
    implementation("org.http4k:http4k-template-handlebars:5.6.3.0")
    implementation("org.http4k:http4k-format-jackson:5.6.3.0")

    testImplementation("org.http4k:http4k-client-jetty:5.6.3.0")
    testImplementation("org.http4k:http4k-client-websocket:5.6.3.0")
    testImplementation("org.http4k:http4k-testing-webdriver:5.6.3.0")
    testImplementation(kotlin("test"))
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
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
    val http4kVersion = "5.6.3.0"
    val seleniumVersion = "4.11.0"
    val chromeVersion = "v114"

    implementation("org.http4k:http4k-core:$http4kVersion")
    implementation("org.http4k:http4k-template-handlebars:$http4kVersion")
    implementation("org.http4k:http4k-format-jackson:$http4kVersion")
    implementation("org.http4k:http4k-server-undertow:$http4kVersion")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    testImplementation(kotlin("test"))
    // TODO: somehow get the latest release from https://github.com/SeleniumHQ/selenium/releases/latest
    testImplementation("org.seleniumhq.selenium:selenium-devtools-$chromeVersion:$seleniumVersion")
    testImplementation("org.seleniumhq.selenium:selenium-chrome-driver:$seleniumVersion")
    testImplementation("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
    testImplementation("com.natpryce:hamkrest:1.8.0.1")
    testImplementation("org.http4k:http4k-client-jetty:$http4kVersion")
}

tasks.test {
    failFast = true
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
application {
    mainClass.set("MainKt")
}

// if I want backing fields:
// https://gist.github.com/dellisd/a1e2ae1a7e6b61590bef4b2542a555a0
// https://kotlinlang.org/docs/whatsnew-eap.html#share-your-feedback-on-the-new-k2-compiler
// this goes into gradle.properties - kotlin.experimental.tryK2=true

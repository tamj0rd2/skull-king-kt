import org.gradle.internal.classpath.Instrumented.systemProperty
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    application
}

group = "com.tamj0rd2"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val seleniumVersion = "4.12.0"
    val chromeVersion = "v114"

    implementation(platform("org.http4k:http4k-bom:5.8.5.1"))
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-format-core")
    implementation("org.http4k:http4k-format-kotlinx-serialization")
    implementation("org.http4k:http4k-server-jetty")
    implementation("org.http4k:http4k-template-handlebars")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("dev.adamko.kxstsgen:kxs-ts-gen-core:0.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    testImplementation(kotlin("test"))
    testImplementation("org.seleniumhq.selenium:selenium-devtools-$chromeVersion:$seleniumVersion")
    testImplementation("org.seleniumhq.selenium:selenium-chrome-driver:$seleniumVersion")
    testImplementation("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
    testImplementation("org.http4k:http4k-client-jetty")
    testImplementation("org.http4k:http4k-client-websocket")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.7.2")
    implementation("org.jsoup:jsoup:1.16.2")
}

tasks.test {
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
    systemProperty("junit.platform.output.capture.stdout", "true")
    systemProperty("junit.platform.output.capture.stderr", "true")
    useJUnitPlatform()
}

tasks.register<JavaExec>("buildFrontend") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("BuildFrontendKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    finalizedBy("buildFrontend")
}

application {
    mainClass.set("com.tamj0rd2.webapp.ServerKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.tamj0rd2.webapp.ServerKt"
    }

    // To avoid the duplicate handling strategy error
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // To add all of the dependencies
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

// if I want backing fields:
// https://gist.github.com/dellisd/a1e2ae1a7e6b61590bef4b2542a555a0
// https://kotlinlang.org/docs/whatsnew-eap.html#share-your-feedback-on-the-new-k2-compiler
// this goes into gradle.properties - kotlin.experimental.tryK2=true

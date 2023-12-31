import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
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

    implementation(project(":common"))

    implementation(platform("org.http4k:http4k-bom:5.8.5.1"))
    implementation("ch.qos.logback:logback-classic:1.4.12")
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-format-core")
    implementation("org.http4k:http4k-format-kotlinx-serialization")
    implementation("org.http4k:http4k-server-jetty")
    implementation("org.http4k:http4k-client-jetty")
    implementation("org.http4k:http4k-template-handlebars")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("dev.adamko.kxstsgen:kxs-ts-gen-core:0.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    testImplementation(kotlin("test"))
    testImplementation("org.seleniumhq.selenium:selenium-devtools-$chromeVersion:$seleniumVersion")
    testImplementation("org.seleniumhq.selenium:selenium-chrome-driver:$seleniumVersion")
    testImplementation("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
    testImplementation("org.http4k:http4k-client-websocket")
    testImplementation("io.kotest:kotest-assertions-json:5.7.2")
    testImplementation("org.jsoup:jsoup:1.16.2")
    testImplementation("io.strikt:strikt-core:0.34.0")
}

tasks.test {
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
    systemProperty("junit.platform.output.capture.stdout", "true")
    systemProperty("junit.platform.output.capture.stderr", "true")
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"

    val frontendProjects = mapOf(
        // TODO: pick a frontend and just stick with it...
        //":frontend-vanilla" to "frontend-vanilla",
        ":frontend-svelte" to "frontend-svelte",
        //":frontend-solid" to "frontend-solid",
    )
        .onEach { dependsOn("${it.key}:build") }
        // a map of build directory to the folder name I want to appear in app/src/resources
        .map { project(it.key).layout.buildDirectory.dir("dist") to it.value }
        // registers the build directory as an input
        .onEach {inputs.dir(it.first) }

    // TODO: apparently it's bad to copy directly to the resources folder, but I don't know how else to solve this
    //   not sure how to make http4k resource loader work if I put the resources elsewhere.
    //   Check this out - https://docs.gradle.org/current/userguide/declaring_dependencies_between_subprojects.html#sec:project_jar_dependencies
    val appPublicResources = File(sourceSets.main.get().resources.srcDirs.single(), "public")
    doFirst {
        frontendProjects.forEach { (frontendDist, folderName) ->
            frontendDist.get().asFile.copyRecursively(appPublicResources.resolve(folderName), true)
        }
    }
}

tasks.processResources {
    println("hi...")
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

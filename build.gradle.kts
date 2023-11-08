import org.gradle.internal.classpath.Instrumented

plugins {
    kotlin("multiplatform") version "1.9.20"
    application
}

group = "com.tamj0rd2"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

val seleniumVersion = "4.12.0"
val chromeVersion = "v114"

application {
    mainClass.set("com.tamj0rd2.webapp.ServerKt")
}

kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
        testRuns.named("test") {
            executionTask.configure {
                systemProperty("junit.jupiter.execution.parallel.enabled", "true")
                systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
                systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
                systemProperty("junit.platform.output.capture.stdout", "true")
                systemProperty("junit.platform.output.capture.stderr", "true")
                useJUnitPlatform()
            }
        }
    }

    sourceSets {
        val commonMain by getting {}
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(project.dependencies.platform("org.http4k:http4k-bom:5.8.5.1"))
                implementation("ch.qos.logback:logback-classic:1.4.11")
                implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")
                implementation("org.http4k:http4k-core")
                implementation("org.http4k:http4k-format-core")
                implementation("org.http4k:http4k-format-jackson")
                implementation("org.http4k:http4k-server-jetty")
                implementation("org.http4k:http4k-template-handlebars")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.seleniumhq.selenium:selenium-devtools-$chromeVersion:$seleniumVersion")
                implementation("org.seleniumhq.selenium:selenium-chrome-driver:$seleniumVersion")
                implementation("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
                implementation("org.http4k:http4k-client-jetty")
                implementation("org.http4k:http4k-client-websocket")
                implementation("io.kotest:kotest-assertions-core-jvm:5.7.2")
            }
        }
    }
}

//tasks.named<Copy>("jvmProcessResources") {
//    val jsBrowserDistribution = tasks.named("jsBrowserDistribution")
//    from(jsBrowserDistribution)
//}

tasks.named<JavaExec>("run") {
    dependsOn(tasks.named<Jar>("jvmJar"))
    classpath(tasks.named<Jar>("jvmJar"))
}

/*
tasks.withType<KotlinCompile> {
    dependsOn("buildFrontend")

    kotlinOptions.jvmTarget = "17"
    kotlinOptions.languageVersion = "1.9"
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

task("buildFrontend") {
    doFirst("npm install") {
        exec {
            workingDir = file("src/frontend")
            commandLine = listOf("npm", "install")
        }

        exec {
            workingDir = file("src/svelte-frontend")
            commandLine = listOf("npm", "install")
        }
    }

    doLast("build js") {
        exec {
            workingDir = file("src/frontend")
            commandLine = listOf("npm", "run", "build")
        }

        exec {
            workingDir = file("src/svelte-frontend")
            commandLine = listOf("npm", "run", "build")
        }
    }
}
*/

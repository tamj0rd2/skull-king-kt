import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.0"

    // https://kotlinlang.org/docs/ksp-quickstart.html#use-your-own-processor-in-a-project
    // https://kotlinlang.org/docs/ksp-quickstart.html#make-ide-aware-of-generated-code
    // https://arrow-kt.io/learn/quickstart/#additional-setup-for-plug-ins - slightly outdated
    id("com.google.devtools.ksp") version "1.9.21-1.0.15"
}

group = "com.tamj0rd2"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.adamko.kxstsgen:kxs-ts-gen-core:0.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")
    api(platform("dev.forkhandles:forkhandles-bom:2.10.2.0"))
    api("dev.forkhandles:values4k")
    implementation("io.arrow-kt:arrow-optics:1.1.3-alpha.44")
    ksp("io.arrow-kt:arrow-optics-ksp-plugin:1.1.3-alpha.44")

    testImplementation(kotlin("test"))
    testImplementation("io.strikt:strikt-core:0.34.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
    kotlinOptions.freeCompilerArgs = listOf("-Xcontext-receivers")
}

tasks.register<JavaExec>("generateTypes") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("tools.GenerateTypesKt")
    standardOutput = ByteArrayOutputStream()

    inputs.dir("src/main/kotlin/com/tamj0rd2")
        .withPropertyName("sourceFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    // https://docs.gradle.org/current/userguide/incremental_build.html#sec:task_input_output_runtime_api
    val outputFolder = layout.buildDirectory.dir("generatedFiles")
    outputs.dir(outputFolder).withPropertyName("outputDir")
    // https://stackoverflow.com/a/50561084
    outputs.cacheIf { true }

    doLast {
        val theOutput = standardOutput.toString().replace("MessageId", "TamId")
        File(outputFolder.get().asFile, "generated_types.ts").writeText(theOutput)
    }
}
//
//gradle.taskGraph.whenReady(closureOf<TaskExecutionGraph> {
//    println("Found task graph: $this")
//    println("Found " + allTasks.size + " tasks.")
//    allTasks.forEach { task ->
//        println(task)
//        task.dependsOn.forEach { dep ->
//            println("  - $dep")
//        }
//    }
//})

//abstract class GenerateTypes : DefaultTask() {
//    @get:Input
//    abstract val greeting: Property<String>
//
//    @TaskAction
//    fun execute() {
//        println("Hello ${greeting.get()} :D")
//    }
//}


// if I want backing fields:
// https://gist.github.com/dellisd/a1e2ae1a7e6b61590bef4b2542a555a0
// https://kotlinlang.org/docs/whatsnew-eap.html#share-your-feedback-on-the-new-k2-compiler
// this goes into gradle.properties - kotlin.experimental.tryK2=true

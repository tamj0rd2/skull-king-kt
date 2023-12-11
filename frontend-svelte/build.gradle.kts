import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("com.github.node-gradle.node") version "7.0.1"
}

group = "com.tamj0rd2"
version = "1.0-SNAPSHOT"

node {
    download = true
    version = "18.2.0"
}

tasks.register("clean") {
    doLast {
        project.delete(layout.buildDirectory)
    }
}

tasks.register<NpmTask>("buildApp") {
    dependsOn("includeGeneratedTypes")
    dependsOn("npmInstall")
    args = listOf("run", "build")

    inputs.files(
        "package.json",
        "package-lock.json",
        "tsconfig.json",
        "tsconfig.node.json",
        "generated_types.ts",
        "vite.config.ts",
        "index.html",
        "svelte.config.js",
    )
    inputs.dir("src")
    inputs.dir("public")
    inputs.dir(fileTree("node_modules").exclude(".cache"))

    outputs.dir(layout.buildDirectory.dir("dist")).withPropertyName("outputDir")
    outputs.cacheIf { true }
}

tasks.register("includeGeneratedTypes") {
    dependsOn(":common:generateTypes")

    val sourceOfGeneratedTypes = project(":common")
        .layout.buildDirectory
        .file("generatedFiles/generated_types.ts")
    inputs.files(sourceOfGeneratedTypes)

    val destination = project.projectDir.resolve("generated_types.ts")
    outputs.files(destination)
    outputs.cacheIf { true }

    doFirst {
        sourceOfGeneratedTypes.get().asFile.copyTo(destination, true)
    }
}

tasks.register<NpmTask>("runDevServer") {
    dependsOn("includeGeneratedTypes")
    dependsOn("npmInstall")
    args = listOf("run", "dev")
}

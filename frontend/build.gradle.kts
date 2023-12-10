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
    dependsOn(":common:generateTypes")
    dependsOn("npmInstall")
    args = listOf("run", "build")

    inputs.files("package.json", "package-lock.json", "tsconfig.json")
    inputs.dir("assets")
    inputs.dir("src")
    inputs.dir(fileTree("node_modules").exclude(".cache"))

    outputs.dir(layout.buildDirectory.dir("dist")).withPropertyName("outputDir")
    outputs.cacheIf { true }

    doFirst {
        project(":common").layout.buildDirectory
            .file("generatedFiles/generated_types.ts")
            .get().asFile
            .copyTo(project.projectDir.resolve("generated_types.ts"), true)
    }
}
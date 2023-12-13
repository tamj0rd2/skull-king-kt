package plugins

import com.github.gradle.node.npm.task.NpmTask
import gradle.kotlin.dsl.accessors._dc82aa4f4be44a03c78038245cc4271c.node
import org.gradle.kotlin.dsl.assign

plugins {
    id("com.github.node-gradle.node")
}

node {
    download = true
    version = "18.2.0"
}

tasks.register("clean") {
    doLast {
        project.delete(layout.buildDirectory)
    }
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

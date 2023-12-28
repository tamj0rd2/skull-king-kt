import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("plugins.frontend")
}

tasks.register<NpmTask>("build") {
    dependsOn("includeGeneratedTypes")
    dependsOn("npmInstall")
    args = listOf("run", "build")

    inputs.files("package.json", "package-lock.json", "tsconfig.json", "generated_types.ts")
    inputs.dir("assets")
    inputs.dir("src")
    inputs.dir(fileTree("node_modules").exclude(".cache"))

    outputs.dir(layout.buildDirectory.dir("dist")).withPropertyName("outputDir")
    outputs.cacheIf { true }
}

import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("plugins.frontend")
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

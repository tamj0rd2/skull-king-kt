plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin(module = "gradle-plugin", version = "1.9.21"))
    // https://mvnrepository.com/artifact/com.github.node-gradle.node/com.github.node-gradle.node.gradle.plugin
    implementation("com.github.node-gradle.node:com.github.node-gradle.node.gradle.plugin:7.0.1")
}

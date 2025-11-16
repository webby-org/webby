import org.gradle.api.tasks.JavaExec

plugins {
    application
}

dependencies {
    implementation(project(":webby-core"))
}

application {
    // Default entrypoint; override via -PmainClass if desired.
    mainClass = providers.gradleProperty("mainClass")
        .orElse("org.webby.examples.HelloWorldExample")
}

val exampleMainClasses = mapOf(
    "runHelloExample" to "org.webby.examples.HelloWorldExample",
    "runEchoExample" to "org.webby.examples.EchoPostExample",
    "runMiddlewareExample" to "org.webby.examples.MiddlewareExample",
    "runHttpsExample" to "org.webby.examples.HttpsExample",
)

exampleMainClasses.forEach { (taskName, mainClassName) ->
    tasks.register<JavaExec>(taskName) {
        group = "application"
        description = "Runs $mainClassName"
        mainClass.set(mainClassName)
        classpath = sourceSets["main"].runtimeClasspath
    }
}

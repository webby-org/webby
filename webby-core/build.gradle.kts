plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testImplementation("org.junit.platform:junit-platform-launcher:6.0.1")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "--add-exports", "java.base/sun.security.x509=ALL-UNNAMED",
        "--add-exports", "java.base/sun.security.tools.keytool=ALL-UNNAMED"
    )
}

tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports", "java.base/sun.security.x509=ALL-UNNAMED",
            "--add-exports", "java.base/sun.security.tools.keytool=ALL-UNNAMED"
        )
    )
}

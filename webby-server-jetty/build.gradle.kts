dependencies {
    implementation(project(":webby-core"))
    implementation(libs.jetty.server)
    implementation(libs.jakarta.servlet)
    testImplementation(libs.bundles.testing)
}

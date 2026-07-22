dependencies {
    api(project(":reqover-core"))
    implementation("io.micrometer:context-propagation:1.1.2")
    implementation("io.projectreactor:reactor-core:3.6.11")
    compileOnly("org.springframework:spring-context:6.1.14")
    compileOnly("org.springframework:spring-webflux:6.1.14")

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

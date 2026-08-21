plugins {
    id("org.springframework.boot")
}

val springBootVersion: String by project
val jacksonVersion: String by project
val log4jVersion: String by project

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    implementation(platform("org.apache.logging.log4j:log4j-bom:$log4jVersion"))
    // One dependency: the starter brings core, report, and both adapters.
    implementation(project(":reqover-spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

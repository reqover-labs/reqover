plugins {
    id("org.springframework.boot")
}

val springBootVersion: String by project
val jacksonVersion: String by project
val log4jVersion: String by project
val tomcatVersion: String by project

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    implementation(platform("org.apache.logging.log4j:log4j-bom:$log4jVersion"))
    // One dependency: the starter brings core, report, and both adapters.
    implementation(project(":reqover-spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    constraints {
        // Keep embedded Tomcat aligned above the Boot BOM's vulnerable patch.
        implementation("org.apache.tomcat.embed:tomcat-embed-core:$tomcatVersion")
        implementation("org.apache.tomcat.embed:tomcat-embed-el:$tomcatVersion")
        implementation("org.apache.tomcat.embed:tomcat-embed-websocket:$tomcatVersion")
    }

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

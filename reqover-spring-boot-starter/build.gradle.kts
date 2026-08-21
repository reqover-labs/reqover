val springBootVersion: String by project
val junitVersion: String by project
val jacksonVersion: String by project
val log4jVersion: String by project

dependencies {
    api(project(":reqover-core"))
    api(project(":reqover-report"))
    api(project(":reqover-spring-mvc"))
    api(project(":reqover-spring-webflux"))

    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("org.springframework:spring-webflux")
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    // Both are ahead of the Spring Boot BOM, which ships versions OSV reports
    // as vulnerable. The starter's web and test starters pull them in.
    testImplementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    testImplementation(platform("org.apache.logging.log4j:log4j-bom:$log4jVersion"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

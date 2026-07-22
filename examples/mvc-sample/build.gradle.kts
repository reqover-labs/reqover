plugins {
    id("org.springframework.boot") version "3.3.5"
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.5"))
    implementation(project(":reqover-core"))
    implementation(project(":reqover-report"))
    implementation(project(":reqover-spring-mvc"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

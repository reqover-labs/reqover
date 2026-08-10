plugins {
    id("org.springframework.boot")
}

val springBootVersion: String by project
val jacksonVersion: String by project
val nettyVersion: String by project

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    implementation(platform("io.netty:netty-bom:$nettyVersion"))
    implementation(project(":reqover-core"))
    implementation(project(":reqover-report"))
    implementation(project(":reqover-spring-webflux"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

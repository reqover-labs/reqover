val asmVersion: String by project
val junitVersion: String by project

dependencies {
    api(project(":reqover-core"))
    implementation("org.ow2.asm:asm:$asmVersion")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

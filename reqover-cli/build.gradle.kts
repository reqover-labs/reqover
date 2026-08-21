plugins {
    id("com.gradleup.shadow") version "9.6.1"
}

val junitVersion: String by project

dependencies {
    implementation(project(":reqover-report"))

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Main-Class" to "io.reqover.cli.ReqoverCli",
            "Implementation-Title" to "reqover-cli",
            "Implementation-Version" to project.version
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(rootProject.file("LICENSE")) {
        into("META-INF")
        rename { "LICENSE-REQOVER" }
    }
    from(rootProject.file("NOTICE")) {
        into("META-INF")
        rename { "NOTICE-REQOVER" }
    }
    from(rootProject.file("THIRD_PARTY_NOTICES.md")) {
        into("META-INF")
    }
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
}

tasks.jar {
    archiveClassifier.set("thin")
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

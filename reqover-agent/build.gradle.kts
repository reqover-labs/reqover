plugins {
    id("com.gradleup.shadow") version "9.6.1"
}

val jacksonVersion: String by project
val junitVersion: String by project

dependencies {
    implementation(project(":reqover-core"))
    implementation(project(":reqover-instrumentation"))

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Premain-Class" to "io.reqover.agent.ReqoverAgent",
            "Can-Redefine-Classes" to "false",
            "Can-Retransform-Classes" to "false"
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
    from(rootProject.file("third-party-licenses/ASM-BSD-3-Clause.txt")) {
        into("META-INF")
        rename { "LICENSE-ASM" }
    }
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
}

tasks.jar {
    archiveClassifier.set("thin")
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("org.objectweb.asm", "io.reqover.agent.internal.asm") {
        skipStringConstants = true
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    dependsOn(tasks.shadowJar)
    dependsOn(":examples:mvc-sample:bootJar")
    dependsOn(":examples:webflux-sample:bootJar")
    systemProperty("reqover.agent.jar", tasks.shadowJar.get().archiveFile.get().asFile.absolutePath)
    systemProperty(
        "reqover.mvc.sample.jar",
        rootProject.layout.projectDirectory.file("examples/mvc-sample/build/libs/mvc-sample-${project.version}.jar").asFile.absolutePath
    )
    systemProperty(
        "reqover.webflux.sample.jar",
        rootProject.layout.projectDirectory.file("examples/webflux-sample/build/libs/webflux-sample-${project.version}.jar").asFile.absolutePath
    )
}

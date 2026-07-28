dependencies {
    implementation(project(":reqover-core"))
    implementation(project(":reqover-instrumentation"))

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes(
            "Premain-Class" to "io.reqover.agent.ReqoverAgent",
            "Can-Redefine-Classes" to "false",
            "Can-Retransform-Classes" to "false"
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().map { dependency ->
            if (dependency.isDirectory) dependency else zipTree(dependency)
        }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
}

tasks.test {
    dependsOn(tasks.jar)
    dependsOn(":examples:mvc-sample:bootJar")
    dependsOn(":examples:webflux-sample:bootJar")
    systemProperty("reqover.agent.jar", tasks.jar.get().archiveFile.get().asFile.absolutePath)
    systemProperty(
        "reqover.mvc.sample.jar",
        rootProject.layout.projectDirectory.file("examples/mvc-sample/build/libs/mvc-sample-${project.version}.jar").asFile.absolutePath
    )
    systemProperty(
        "reqover.webflux.sample.jar",
        rootProject.layout.projectDirectory.file("examples/webflux-sample/build/libs/webflux-sample-${project.version}.jar").asFile.absolutePath
    )
}

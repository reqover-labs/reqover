plugins {
    `java-library`
}

allprojects {
    group = "io.reqover"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

val springBootVersion = "3.3.5"
val springFrameworkVersion = "6.1.14"
val reactorVersion = "3.6.11"
val micrometerContextPropagationVersion = "1.1.2"
val asmVersion = "9.7.1"

tasks.register("cyclonedxBom") {
    group = "reporting"
    description = "Generates a lightweight CycloneDX 1.6 JSON SBOM for Reqover dependencies."

    val outputFile = layout.buildDirectory.file("reports/bom/reqover-sbom.json")
    outputs.file(outputFile)

    doLast {
        val components = linkedMapOf<String, Triple<String, String, String>>()

        subprojects.forEach { project ->
            project.configurations
                .matching { configuration -> configuration.isCanBeResolved }
                .forEach { configuration ->
                    runCatching {
                        configuration.resolvedConfiguration.firstLevelModuleDependencies.forEach { dependency ->
                            fun visit(dep: org.gradle.api.artifacts.ResolvedDependency) {
                                val key = "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}"
                                components.putIfAbsent(key, Triple(dep.moduleGroup, dep.moduleName, dep.moduleVersion))
                                dep.children.forEach(::visit)
                            }
                            visit(dependency)
                        }
                    }
                }
        }

        fun escape(value: String): String = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

        val componentJson = components.values
            .sortedWith(compareBy<Triple<String, String, String>> { it.first }.thenBy { it.second }.thenBy { it.third })
            .joinToString(",\n") { component ->
                val group = escape(component.first)
                val name = escape(component.second)
                val version = escape(component.third)
                """
                {
                  "type": "library",
                  "group": "$group",
                  "name": "$name",
                  "version": "$version",
                  "purl": "pkg:maven/$group/$name@$version"
                }""".trimIndent()
            }

        val bom = """
            {
              "bomFormat": "CycloneDX",
              "specVersion": "1.6",
              "version": 1,
              "metadata": {
                "component": {
                  "type": "library",
                  "group": "${rootProject.group}",
                  "name": "${rootProject.name}",
                  "version": "${rootProject.version}"
                }
              },
              "components": [
            $componentJson
              ]
            }
        """.trimIndent()

        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(bom)
        logger.lifecycle("Generated SBOM: ${file.absolutePath}")
    }
}

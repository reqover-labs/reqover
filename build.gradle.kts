import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.cyclonedx.model.License
import org.cyclonedx.model.LicenseChoice
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-library`
    id("org.cyclonedx.bom") version "3.3.0"
}

allprojects {
    group = "io.reqover"
    version = "0.1.1"

    tasks.withType<org.cyclonedx.gradle.BaseCyclonedxTask>().configureEach {
        licenseChoice.set(LicenseChoice().apply {
            addLicense(License().apply {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            })
        })
    }
}

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    if (!path.startsWith(":examples") && path != ":reqover-agent") {
        apply(plugin = "maven-publish")
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    pom {
                        name.set(project.name)
                        description.set("Reqover request-scoped runtime coverage attribution module")
                        url.set("https://github.com/reqover-labs/reqover")
                        licenses {
                            license {
                                name.set("Apache License 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("TaeHuiKKIM")
                                name.set("김태희")
                                url.set("https://github.com/TaeHuiKKIM")
                            }
                            developer {
                                id.set("lsmin3388")
                                name.set("이상민")
                                url.set("https://github.com/lsmin3388")
                            }
                        }
                        scm {
                            connection.set("scm:git:https://github.com/reqover-labs/reqover.git")
                            developerConnection.set("scm:git:ssh://git@github.com/reqover-labs/reqover.git")
                            url.set("https://github.com/reqover-labs/reqover")
                        }
                    }
                }
            }
        }
    }
}

val reqoverGroup = group.toString()

tasks.cyclonedxBom {
    jsonOutput = layout.buildDirectory.file("reports/bom/reqover.cdx.json")
    xmlOutput.unsetConvention()
    includeBomSerialNumber = true
    includeLicenseText = false
    componentGroup = project.group.toString()
    componentName = rootProject.name
    componentVersion = project.version.toString()

    doLast {
        val bomFile = jsonOutput.get().asFile
        @Suppress("UNCHECKED_CAST")
        val bom = JsonSlurper().parse(bomFile) as MutableMap<String, Any?>
        val apacheLicense = listOf(
            mapOf(
                "license" to mapOf(
                    "id" to "Apache-2.0",
                    "url" to "https://www.apache.org/licenses/LICENSE-2.0.txt"
                )
            )
        )

        @Suppress("UNCHECKED_CAST")
        val metadata = bom["metadata"] as MutableMap<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val rootComponent = metadata["component"] as MutableMap<String, Any?>
        rootComponent["licenses"] = apacheLicense

        @Suppress("UNCHECKED_CAST")
        val components = bom["components"] as List<MutableMap<String, Any?>>
        components
            .filter { it["group"] == reqoverGroup }
            .forEach { it["licenses"] = apacheLicense }

        bomFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(bom)) + "\n")
    }
}

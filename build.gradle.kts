import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.cyclonedx.model.License
import org.cyclonedx.model.LicenseChoice
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

plugins {
    `java-library`
    id("org.cyclonedx.bom") version "3.4.1"
}

allprojects {
    group = "io.reqover"
    version = "0.2.0"

    tasks.withType<org.cyclonedx.gradle.BaseCyclonedxTask>().configureEach {
        licenseChoice.set(LicenseChoice().apply {
            addLicense(License().apply {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            })
        })
    }
}

/**
 * Modules published as libraries. The agent and the CLI are shaded executables
 * that ship in the release bundle, and the samples are demos, so neither is
 * something to compile against.
 */
val publishedModules = mapOf(
    "reqover-core" to "Request-scoped coverage buckets and the record store at the heart of Reqover",
    "reqover-instrumentation" to "ASM bytecode instrumentation that inserts Reqover's method-entry probes",
    "reqover-report" to "Report aggregation, reverse lookup, impact analysis, and JSON/HTML rendering",
    "reqover-spring-mvc" to "Spring MVC adapter binding coverage buckets to servlet requests",
    "reqover-spring-webflux" to "Spring WebFlux adapter that keeps attribution across Reactor thread hops",
    "reqover-spring-boot-starter" to "Spring Boot starter wiring Reqover with a single dependency"
)

/** Where {@code centralBundle} stages the Maven Central upload layout. */
val centralBundleRepository = layout.buildDirectory.dir("central-bundle/repository")

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

    val moduleDescription = publishedModules[name]
    if (moduleDescription != null) {
        apply(plugin = "maven-publish")

        // Maven Central requires a Javadoc jar. Missing comments are not an
        // error here, but malformed markup is: a release should not be the
        // first time we notice.
        extensions.configure<JavaPluginExtension> {
            withJavadocJar()
        }
        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).apply {
                addStringOption("Xdoclint:all,-missing", "-quiet")
                encoding = "UTF-8"
                charSet = "UTF-8"
            }
        }

        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    pom {
                        name.set(project.name)
                        description.set(moduleDescription)
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

            repositories {
                // A local directory rather than a live server: the release job
                // stages the exact bytes, checks them, then uploads the zip.
                maven {
                    name = "centralBundle"
                    url = centralBundleRepository.get().asFile.toURI()
                }
            }
        }

        // Signing is skipped when no key is configured, so a clone with no
        // secrets still builds and publishes to mavenLocal.
        val signingKey = providers.environmentVariable("REQOVER_SIGNING_KEY").orNull
        val signingPassword = providers.environmentVariable("REQOVER_SIGNING_PASSWORD").orNull
        if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
            apply(plugin = "signing")
            extensions.configure<SigningExtension> {
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(extensions.getByType<PublishingExtension>().publications["mavenJava"])
            }
        }
    }
}

/**
 * Stages every published module into one directory laid out the way Maven
 * Central expects, then zips it for the Central Portal upload API.
 *
 * <p>Run with the signing key present, or Central will reject the bundle:
 * {@code REQOVER_SIGNING_KEY=... REQOVER_SIGNING_PASSWORD=... ./gradlew centralBundle}
 */
val centralBundle by tasks.registering(Zip::class) {
    group = "publishing"
    description = "Builds the signed Maven Central upload bundle for every published module."

    dependsOn(publishedModules.keys.map { ":$it:publishMavenJavaPublicationToCentralBundleRepository" })
    doFirst {
        if (System.getenv("REQOVER_SIGNING_KEY").isNullOrBlank()) {
            logger.warn(
                "[reqover] REQOVER_SIGNING_KEY is not set: the bundle will have no .asc signatures "
                    + "and Maven Central will reject it."
            )
        }
    }

    from(centralBundleRepository) {
        // Central generates its own metadata; an uploaded copy only invalidates the bundle.
        exclude("**/maven-metadata.xml*")
    }
    archiveFileName.set("reqover-$version-central-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("central-bundle"))
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

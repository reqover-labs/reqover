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

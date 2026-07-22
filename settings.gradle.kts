pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "reqover"

include("reqover-core")
include("reqover-report")
include("reqover-spring-mvc")
include("reqover-spring-webflux")
include("reqover-instrumentation")
include("reqover-agent")
include("examples:mvc-sample")
include("examples:webflux-sample")

/*
 * Copyright 2026 Reqover contributors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

val springBootVersion: String by project
val junitVersion: String by project
val jacksonVersion: String by project
val log4jVersion: String by project
val tomcatVersion: String by project

dependencies {
    api(project(":reqover-core"))
    api(project(":reqover-report"))
    api(project(":reqover-spring-mvc"))
    api(project(":reqover-spring-webflux"))

    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("org.springframework:spring-webflux")
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    // Both are ahead of the Spring Boot BOM, which ships versions OSV reports
    // as vulnerable. The starter's web and test starters pull them in.
    testImplementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    testImplementation(platform("org.apache.logging.log4j:log4j-bom:$log4jVersion"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    constraints {
        // Test-only: do not impose the sample server on published starter users.
        testImplementation("org.apache.tomcat.embed:tomcat-embed-core:$tomcatVersion")
        testImplementation("org.apache.tomcat.embed:tomcat-embed-el:$tomcatVersion")
        testImplementation("org.apache.tomcat.embed:tomcat-embed-websocket:$tomcatVersion")
    }
}

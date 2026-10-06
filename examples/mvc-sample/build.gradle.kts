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

plugins {
    id("org.springframework.boot")
}

val springBootVersion: String by project
val jacksonVersion: String by project
val log4jVersion: String by project
val tomcatVersion: String by project

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    implementation(platform("org.apache.logging.log4j:log4j-bom:$log4jVersion"))
    // One dependency: the starter brings core, report, and both adapters.
    implementation(project(":reqover-spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    constraints {
        // Keep embedded Tomcat aligned above the Boot BOM's vulnerable patch.
        implementation("org.apache.tomcat.embed:tomcat-embed-core:$tomcatVersion")
        implementation("org.apache.tomcat.embed:tomcat-embed-el:$tomcatVersion")
        implementation("org.apache.tomcat.embed:tomcat-embed-websocket:$tomcatVersion")
    }

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

import java.security.MessageDigest

plugins {
    kotlin("jvm") version "1.9.20"
    `java-library`
    `maven-publish`
    signing
}

group = "io.github.iamkoch"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // JUnit 5
    implementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    implementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    implementation("org.junit.platform:junit-platform-engine:1.10.0")

    // Test dependencies
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "kbehave"

            pom {
                name.set("KBehave")
                description.set("A Kotlin version of xBehave.net - a JUnit 5 extension for describing each step in a test with natural language")
                url.set("https://github.com/iamkoch/KBehave")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("iamkoch")
                        name.set("Koch")
                        url.set("https://github.com/iamkoch")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/iamkoch/KBehave.git")
                    developerConnection.set("scm:git:ssh://github.com/iamkoch/KBehave.git")
                    url.set("https://github.com/iamkoch/KBehave")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    val signingKey = findProperty("signingKey") as String? ?: System.getenv("SIGNING_KEY")
    val signingPassword = findProperty("signingPassword") as String? ?: System.getenv("SIGNING_PASSWORD")

    // Only sign if credentials are present (skip for local publishing)
    setRequired {
        signingKey != null && signingPassword != null
    }

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    sign(publishing.publications["maven"])
}

// Generate checksums for Maven local repository (needed for Bazel)
tasks.register("generateChecksums") {
    group = "publishing"
    description = "Generate SHA-1 and MD5 checksums for locally published artifacts"

    doLast {
        val mavenLocalDir = file("${System.getProperty("user.home")}/.m2/repository")
        val groupPath = project.group.toString().replace('.', '/')
        val artifactPath = "$groupPath/kbehave/${project.version}"
        val artifactDir = mavenLocalDir.resolve(artifactPath)

        println("Looking for artifacts in: ${artifactDir.absolutePath}")

        if (artifactDir.exists()) {
            val files = artifactDir.listFiles()?.filter {
                it.extension in listOf("jar", "pom", "module")
            } ?: emptyList()

            println("Found ${files.size} files to process")

            files.forEach { publishedFile ->
                println("Processing: ${publishedFile.name}")

                // Generate SHA-1
                val sha1Bytes = MessageDigest.getInstance("SHA-1").digest(publishedFile.readBytes())
                val sha1 = sha1Bytes.joinToString("") { "%02x".format(it) }
                val sha1File = publishedFile.resolveSibling("${publishedFile.name}.sha1")
                sha1File.writeText(sha1)
                println("  Created: ${sha1File.name}")

                // Generate MD5
                val md5Bytes = MessageDigest.getInstance("MD5").digest(publishedFile.readBytes())
                val md5 = md5Bytes.joinToString("") { "%02x".format(it) }
                val md5File = publishedFile.resolveSibling("${publishedFile.name}.md5")
                md5File.writeText(md5)
                println("  Created: ${md5File.name}")
            }
            println("Checksum generation complete!")
        } else {
            println("ERROR: Artifact directory does not exist: ${artifactDir.absolutePath}")
            println("Please run 'publishToMavenLocal' first")
        }
    }
}

tasks.named("publishToMavenLocal") {
    finalizedBy("generateChecksums")
}
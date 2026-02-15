import java.security.MessageDigest

plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.dokka") version "1.9.20"
    `java-library`
    `maven-publish`
    signing
}

group = "io.github.iamkoch"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // JUnit BOM for version alignment across all scopes
    implementation(platform("org.junit:junit-bom:5.14.2"))
    testImplementation(platform("org.junit:junit-bom:5.14.2"))

    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // JUnit Platform (engine SPI for KBehave TestEngine)
    implementation("org.junit.platform:junit-platform-engine")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.platform:junit-platform-testkit")
    testImplementation("org.junit.platform:junit-platform-console")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
    // Exclude engine test fixture inner classes from direct discovery.
    // They contain intentionally-failing @Scenario methods and are executed
    // only via junit-platform-testkit inside KBehaveEngineTest.
    exclude("**/engine/KBehaveEngineTest\$*")
}

java {
    withSourcesJar()
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(dokkaJavadocJar)
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
                        email.set("ant@iamkoch.com")
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
            name = "CentralPortal"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("ossrhUsername") as String? ?: System.getenv("MAVEN_USERNAME")
                password = findProperty("ossrhPassword") as String? ?: System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}

signing {
    val signingKey = findProperty("signingKey") as String? ?: System.getenv("SIGNING_KEY")
    val signingPassword = findProperty("signingPassword") as String? ?: System.getenv("SIGNING_PASSWORD")

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

                val sha1Bytes = MessageDigest.getInstance("SHA-1").digest(publishedFile.readBytes())
                val sha1 = sha1Bytes.joinToString("") { "%02x".format(it) }
                val sha1File = publishedFile.resolveSibling("${publishedFile.name}.sha1")
                sha1File.writeText(sha1)
                println("  Created: ${sha1File.name}")

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

plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.dokka") version "1.9.20"
    `maven-publish`
    signing
}

group = "dev.ocpd.kairs"

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.ocpd.slf4k:slf4k:0.2.0")
    testImplementation("ch.qos.logback:logback-classic:1.5.6")
    testImplementation(kotlin("test-junit5"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkaJavadoc"))
}

val collectNative = task<Copy>("collectNative") {
    group = "build"
    dependsOn(":native:build")
    from(project(":native").layout.buildDirectory.dir("libs"))
    into(sourceSets.main.map {
        val resOutDir = it.output.resourcesDir ?: error("Expect to have resource output dir")
        resOutDir.resolve("native")
    })
}

tasks.processResources {
    val skip = providers.gradleProperty("kairs.skipNative").map(String::toBoolean).getOrElse(false)
    if (!skip) {
        dependsOn(collectNative)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "kairs"
                description = "Kotlin AI powered by Rust"
                url = "https://github.com/ocpddev/kairs"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                scm {
                    url = "https://github.com/ocpddev/kairs"
                }
                developers {
                    developer {
                        id = "sola"
                        name = "Sola"
                        email = "sola@ocpd.dev"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            val releaseUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")

            url = if (version.toString().endsWith("-SNAPSHOT")) snapshotUrl else releaseUrl

            credentials {
                username = project.findSecret("ossrh.username", "OSSRH_USERNAME")
                password = project.findSecret("ossrh.password", "OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    val key = findSecret("ocpd.sign.key", "OCPD_SIGN_KEY")
    if (key != null) {
        val keyId = findSecret("ocpd.sign.key.id", "OCPD_SIGN_KEY_ID")
        val passphrase = findSecret("ocpd.sign.passphrase", "OCPD_SIGN_PASSPHRASE") ?: ""
        useInMemoryPgpKeys(keyId, key, passphrase)
    }
    sign(publishing.publications["maven"])
}

fun Project.findSecret(key: String, env: String): String? =
    providers.gradleProperty(key).orElse(providers.environmentVariable(env)).orNull

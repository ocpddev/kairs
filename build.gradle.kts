plugins {
    kotlin("jvm") version "1.9.23"
}

group = "dev.ocpd.kairs"

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.ocpd.slf4k:slf4k:0.2.0")
    testImplementation("ch.qos.logback:logback-classic:1.5.3")
    testImplementation(kotlin("test-junit5"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
    }
}

val buildNative = task<Copy>("buildNative") {
    dependsOn(":native:build")
    from(project(":native").cargo().libOutputPath(project.name))
    into(sourceSets.main.map {
        val resOutDir = it.output.resourcesDir ?: error("Expect to have resource output dir")
        resOutDir.resolve("native/${project.name}/${Platform.current().identifier()}")
    })
}

tasks.processResources {
    dependsOn(buildNative)
}

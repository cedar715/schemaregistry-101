plugins {
    application
}

group = "io.github.cedar715"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.picocli)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.assertj)
}

val mainClassName = "io.github.cedar715.srbackup.SrBackup"

application {
    mainClass = mainClassName
    applicationName = "sr-backup"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// One self-contained jar, so it runs anywhere with just a JRE:
//   java -jar build/libs/sr-backup.jar --help
tasks.jar {
    archiveFileName = "sr-backup.jar"
    manifest {
        attributes(
            "Main-Class" to mainClassName,
            "Implementation-Version" to project.version, // shown by --version
        )
    }
    from(configurations.runtimeClasspath.map { cp -> cp.map { zipTree(it) } })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
}

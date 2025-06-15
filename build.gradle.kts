plugins {
    kotlin("jvm") version "2.1.20"
    application
    id("org.graalvm.buildtools.native") version "0.10.6"
}

group = "com.vikbytes"

version = "1.1.0"

tasks.register("generateVersionProperties") {
    doLast {
        file("src/main/resources/version.properties").apply {
            parentFile.mkdirs()
            writeText("version=${project.version}")
        }
    }
}

tasks.compileKotlin { dependsOn("generateVersionProperties") }

repositories { mavenCentral() }

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-java:3.1.3")
    implementation("com.github.ajalt.clikt:clikt:3.5.2")
    implementation("org.slf4j:slf4j-nop:2.0.17")
    implementation("org.hdrhistogram:HdrHistogram:2.1.12")
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(21) }

application {
    mainClass.set("com.vikbytes.MainKt")
    applicationDefaultJvmArgs = listOf("-Dorg.slf4j.simpleLogger.defaultLogLevel=none")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("kanon-${project.version}")
            mainClass.set("com.vikbytes.MainKt")

            buildArgs.add("--no-fallback")
            buildArgs.add("--enable-url-protocols=http,https")
            buildArgs.add("-H:+ReportExceptionStackTraces")

            //            buildArgs.add("-O2") // Speed optimization
            buildArgs.add("-Ob") // Binary size optimization

            buildArgs.add("-H:IncludeResources=.*\\.properties")

            buildArgs.add("-H:ReflectionConfigurationFiles=${project.projectDir}/reflection-config.json")

            buildArgs.add("--enable-all-security-services")

            buildArgs.add("-Dfile.encoding=UTF-8")
            buildArgs.add("-Dsun.stdout.encoding=UTF-8")
            buildArgs.add("-Dsun.stderr.encoding=UTF-8")

            buildArgs.add(
                "--initialize-at-build-time=io.ktor,kotlinx.coroutines,kotlin,org.slf4j,kotlinx.io.bytestring.ByteString,kotlinx.io.bytestring.ByteString\$Companion,kotlinx.io.Buffer")

            // Verbose output for debugging build issues
            // buildArgs.add("--verbose")
        }
    }
}

tasks.register("createReflectionConfig") {
    doLast {
        file("reflection-config.json")
            .writeText(
                """
        [
          {
            "name": "com.vikbytes.KanonCommand",
            "allDeclaredConstructors": true,
            "allPublicConstructors": true,
            "allDeclaredMethods": true,
            "allPublicMethods": true,
            "allDeclaredFields": true,
            "allPublicFields": true
          },
          {
            "name": "kotlin.reflect.jvm.internal.ReflectionFactoryImpl",
            "allDeclaredConstructors": true
          },
          {
            "name": "io.ktor.client.engine.cio.CIOEngineContainer",
            "allPublicMethods": true,
            "allPublicConstructors": true
          }
        ]
        """
                    .trimIndent())
    }
}

tasks.named("nativeCompile") { dependsOn("createReflectionConfig") }

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("fat")
    archiveVersion.set(project.version.toString())
    archiveBaseName.set("kanon")

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) } })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "com.vikbytes.MainKt"
        attributes["Implementation-Version"] = project.version
    }
}

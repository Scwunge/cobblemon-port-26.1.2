/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `java-library`
    alias(libs.plugins.loom)
    alias(libs.plugins.kotlin.jvm)
}

val mcVersion = property("mc_version").toString()
val javaVersion = property("java_version").toString().toInt()

group = property("group").toString()
version = "${property("mod_version")}+$mcVersion"
base.archivesName.set("Cobblemon-fabric")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
}

kotlin {
    jvmToolchain(javaVersion)
}

repositories {
    mavenCentral()
    maven("${rootProject.projectDir}/deps")
    maven("https://maven.blamejared.com/") // JEI
    maven("https://maven.terraformersmc.com/releases/") // Mod Menu
    maven("https://maven.gegy.dev/releases/") // LambDynamicLights
    maven("https://api.modrinth.com/maven")
}

loom {
    accessWidenerPath.set(file("src/main/resources/cobblemon-common.accesswidener"))

    runs {
        named("client") {
            runDir = "runClient"
            programArgs("--username=AshKetchum", "--uuid=93e4e551-589a-41cb-ab2d-435266c8e035")
        }
        named("server") {
            runDir = "runServer"
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.kotlin)
    implementation(libs.fabric.permissions)
    include(libs.fabric.permissions)

    implementation(libs.bundles.kotlin)

    // MoLang runtime (local maven in /deps), shipped inside the jar.
    api(libs.molang)
    include(libs.molang)

    // Showdown battle engine runs on GraalJS.
    implementation(libs.bundles.graal)
    include(libs.bundles.graal)

    // Optional MongoDB storage backend.
    compileOnly(libs.bundles.mongo)
    include(libs.bundles.mongo)

    // Integrations (optional at runtime)
    compileOnly(libs.jei.common.api)
    compileOnly(libs.lambDynamicLights)
    implementation(libs.mod.menu)

    testImplementation(libs.bundles.unitTesting)
}

// CobblemonBuildDetails used to be generated with Blossom from a Pebble template.
val generateBuildDetails by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/buildDetails/kotlin")
    val modVersion = project.version.toString()
    val snapshot = project.property("snapshot").toString() == "true"
    val header = file("HEADER").readLines().joinToString("\n") { if (it.isEmpty()) " *" else " * $it" }
    inputs.property("version", modVersion)
    inputs.property("snapshot", snapshot)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/cobblemon/mod/common/CobblemonBuildDetails.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |/*
            |$header
            | */
            |
            |package com.cobblemon.mod.common
            |
            |object CobblemonBuildDetails {
            |
            |    const val MOD_ID = "cobblemon"
            |    const val VERSION = "$modVersion"
            |    const val SNAPSHOT = $snapshot
            |
            |    // Read at runtime rather than templated in, so a new commit doesn't invalidate compiled code.
            |    private val git = java.util.Properties().apply {
            |        CobblemonBuildDetails::class.java.getResourceAsStream("/cobblemon-git.properties")?.use(::load)
            |    }
            |    @JvmField val GIT_COMMIT: String = git.getProperty("commit", "Not Specified")
            |    @JvmField val BRANCH: String = git.getProperty("branch", "Not Specified")
            |
            |    fun smallCommitHash(): String {
            |        return this.GIT_COMMIT.substring(0, 7)
            |    }
            |}
            |""".trimMargin()
        )
    }
}
sourceSets.main {
    kotlin.srcDir(generateBuildDetails)
}

val gitCommit: String = providers.exec {
    commandLine("git", "rev-parse", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim() }.getOrElse("unknown")

val writeGitInfo by tasks.registering(WriteProperties::class) {
    destinationFile = layout.buildDirectory.file("generated/gitInfo/cobblemon-git.properties")
    property("commit", gitCommit.ifEmpty { "unknown" })
    property("branch", "port/$mcVersion")
}
sourceSets.main { output.dir(mapOf("builtBy" to writeGitInfo), layout.buildDirectory.dir("generated/gitInfo")) }

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(javaVersion)
        options.compilerArgs.add("-Xlint:-processing,-classfile,-serial")
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
            freeCompilerArgs.addAll("-Xjvm-default=all-compatibility", "-Xsuppress-version-warnings")
        }
    }

    processResources {
        exclude("data/**/README.md", "**/create_block_files.py")
        val props = mapOf(
            "version" to project.version.toString(),
            "fabric_loader_version" to libs.versions.fabric.loader.get(),
            "fabric_api_version" to libs.versions.fabric.api.get(),
            "minecraft_version" to mcVersion,
            "java_version" to javaVersion.toString(),
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
    }

    jar {
        from("LICENSE")
    }

    named<Jar>("sourcesJar") {
        exclude("assets/**", "data/**", "datapacks/**", "resourcepacks/**")
    }

    test {
        useJUnitPlatform()
    }
}

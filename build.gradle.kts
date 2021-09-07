@file:Suppress("UNUSED_EXPRESSION")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
	val kotlinVersion : String by project
	val gradlePluginsVersion : String by project

	repositories {
		mavenLocal()
		mavenCentral()
		maven(url = uri( "https://jitpack.io"))
		maven(url = uri( "https://software.r3.com/artifactory/corda-dev"))
		maven(url = uri( "https://software.r3.com/artifactory/corda-releases"))
		maven(url = uri( "https://software.r3.com/artifactory/corda"))
		maven(url = uri( "https://software.r3.com/artifactory/corda-lib"))
		maven(url = uri( "https://repo.gradle.org/gradle/libs-releases-local/"))
	}

	dependencies {
		classpath("net.corda.plugins", "cordformation", gradlePluginsVersion)
		classpath("org.jetbrains.kotlin", "kotlin-gradle-plugin", kotlinVersion)
		classpath("net.corda.plugins","cordapp", gradlePluginsVersion)
		classpath("net.corda.plugins", "quasar-utils", gradlePluginsVersion)
	}
}

plugins {
	kotlin("jvm") version "1.4.20"
}

dependencies {
	implementation(kotlin("stdlib"))
}

allprojects {
	val kotlinVersion : String by project

	apply(plugin = "kotlin")

	repositories {
		mavenCentral()
		jcenter()
		maven(url = uri( "https://jitpack.io"))
		maven(url = uri( "https://software.r3.com/artifactory/corda-dev"))
		maven(url = uri( "https://software.r3.com/artifactory/corda-releases"))
		maven(url = uri( "https://software.r3.com/artifactory/corda"))
		maven(url = uri( "https://software.r3.com/artifactory/corda-lib"))
		maven(url = uri( "https://repo.gradle.org/gradle/libs-releases-local/"))
	}

	group = "com.r3"
	version = "0.0.1-SNAPSHOT"
	java.sourceCompatibility = JavaVersion.VERSION_1_8
	java.targetCompatibility = JavaVersion.VERSION_1_8

	tasks.withType<KotlinCompile> {
		sourceCompatibility = JavaVersion.VERSION_1_8.toString()
		targetCompatibility = JavaVersion.VERSION_1_8.toString()

		kotlinOptions {
			freeCompilerArgs = listOf("-Xjsr305=strict")
			jvmTarget = JavaVersion.VERSION_1_8.toString()
			languageVersion = "1.4"
			apiVersion = "1.4"
			javaParameters = true
		}
	}

	tasks.withType<Test> {
		useJUnitPlatform()
	}

	configurations.all {
		resolutionStrategy {
			// Force dependencies to use the same version of Kotlin
			force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
			force("org.jetbrains.kotlin:kotlin-stdlib-jre7:$kotlinVersion")
			force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
			force("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")
			force("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

			cacheChangingModulesFor( 0, "seconds")
		}
	}
}




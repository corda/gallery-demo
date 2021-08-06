import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.5.21"
}

allprojects {
	apply(
		plugin = "kotlin"
	)

	repositories {
		mavenCentral()
	}

	group = "com.r3"
	version = "0.0.1-SNAPSHOT"
	java.sourceCompatibility = JavaVersion.VERSION_1_8

	tasks.withType<KotlinCompile> {
		kotlinOptions {
			freeCompilerArgs = listOf("-Xjsr305=strict")
			jvmTarget = JavaVersion.VERSION_1_8.toString()
			languageVersion = "1.5"
			apiVersion = "1.5"
		}
	}

	tasks.withType<Test> {
		useJUnitPlatform()
	}
}




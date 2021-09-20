plugins {
    id("net.corda.plugins.cordapp")
    id("net.corda.plugins.quasar-utils")
}

val cordaPlatformVersion : String by project
val cordaCoreReleaseGroup : String by project
val cordaCoreVersion : String by project
val gradlePluginsVersion : String by project
val tokensReleaseVersion : String by project
val tokensReleaseGroup : String by project

cordapp {
    val cordaLedgerReleaseVersion : String by rootProject

    targetPlatformVersion(cordaPlatformVersion.toInt())
    minimumPlatformVersion(cordaPlatformVersion.toInt())
    workflow {
        name("Gallery Workflows")
        vendor("Corda Open Source")
        licence("Apache License, Version 2.0")
        versionId(cordaLedgerReleaseVersion.toInt())
    }
    signing {
        enabled(false)
    }
}

dependencies {
    cordaCompile(cordaCoreReleaseGroup,"corda-core", cordaCoreVersion)
    cordapp(tokensReleaseGroup,"tokens-contracts", tokensReleaseVersion)
    cordapp(tokensReleaseGroup,"tokens-workflows", tokensReleaseVersion)
    cordaRuntime(cordaCoreReleaseGroup, "corda", cordaCoreVersion)
    testImplementation(cordaCoreReleaseGroup, "corda-node-driver", cordaCoreVersion)

    cordapp(project(":gallery-contracts"))
}

tasks.jar {
    archiveBaseName.set("gallery-workflows")
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

configurations["integrationTestImplementation"].extendsFrom(configurations.testImplementation.get())
configurations["integrationTestCompile"].extendsFrom(configurations.testCompile.get())
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

val integrationTest = task<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    // performance
    maxHeapSize = "4G"
    setForkEvery(4)

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath

    useJUnitPlatform {
        includeEngines("junit-jupiter", "junit-vintage")
    }
}
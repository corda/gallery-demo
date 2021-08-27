plugins {
    id("net.corda.plugins.cordapp")
}

val cordaPlatformVersion : String by project
val cordaCoreReleaseGroup : String by project
val cordaCoreVersion : String by project
val gradlePluginsVersion : String by project

cordapp {
    val cordaLedgerReleaseVersion : String by rootProject

    targetPlatformVersion(cordaPlatformVersion.toInt())
        minimumPlatformVersion(cordaPlatformVersion.toInt())
        contract {
            name("Gallery Contracts")
            vendor("Corda Open Source")
            licence("Apache License, Version 2.0")
            versionId(cordaLedgerReleaseVersion.toInt())
        }
}

dependencies {
    cordaCompile(cordaCoreReleaseGroup,"corda-core", cordaCoreVersion)
    cordaCompile("com.r3.corda.lib.tokens:tokens-contracts:1.2.2")
    cordaCompile("com.r3.corda.lib.tokens:tokens-workflows:1.2.2")
    testImplementation(cordaCoreReleaseGroup, "corda-node-driver", cordaCoreVersion)
}

tasks.jar {
    archiveBaseName.set("gallery-contracts")
}
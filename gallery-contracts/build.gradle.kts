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
    signing {
        enabled(false)
    }
}

dependencies {
    cordaCompile(cordaCoreReleaseGroup,"corda-core", cordaCoreVersion)
    testImplementation(cordaCoreReleaseGroup, "corda-node-driver", cordaCoreVersion)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.7")
}

tasks.jar {
    archiveBaseName.set("gallery-contracts")
}
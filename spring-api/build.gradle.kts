plugins {
    id("org.springframework.boot") version "2.5.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("plugin.spring") version "1.4.20"
}

val cordaCoreReleaseGroup : String by project
val cordaCoreVersion : String by project

dependencies {
    implementation(cordaCoreReleaseGroup, "corda-rpc", cordaCoreVersion)
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

task<JavaExec>("runSpringApp") {
    group = "deployment"
    classpath = sourceSets.main.get().runtimeClasspath
    main = "com.r3.gallery.broker.GalleryBrokerApplicationKt"
}

tasks.bootJar {
    archiveBaseName.set("spring-api")
}
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

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation( "org.mockito:mockito-inline:2.21.0")
    testImplementation("com.nhaarman:mockito-kotlin:1.6.0") {
        exclude( "org.jetbrains.kotlin")
        exclude ("org.mockito")
    }
}

task<JavaExec>("runSpringApp") {
    group = "deployment"
    classpath = sourceSets.main.get().runtimeClasspath
    main = "com.r3.gallery.broker.GalleryBrokerApplicationKt"
}

tasks.bootJar {
    archiveBaseName.set("spring-api")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
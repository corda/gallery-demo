plugins {
    id("net.corda.plugins.cordapp")
    id("org.springframework.boot") version "2.5.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("plugin.spring") version "1.4.20"
}

val cordaCoreReleaseGroup : String by project
val cordaCoreVersion : String by project

dependencies {
    implementation(cordaCoreReleaseGroup, "corda-rpc", cordaCoreVersion)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    // https://mvnrepository.com/artifact/org.mock-server/mockserver-client
    implementation("org.mock-server:mockserver-client:3.2")
    runtimeOnly("com.h2database:h2")

    cordaCompile(project(":gallery-contracts"))
    cordaCompile(project(":gallery-workflows"))
    cordaCompile(cordaCoreReleaseGroup, "corda-rpc", cordaCoreVersion)
    cordaCompile(cordaCoreReleaseGroup, "corda-jackson", cordaCoreVersion)

    // https://mvnrepository.com/artifact/org.mock-server/mockserver-netty
    testImplementation("org.mock-server:mockserver-netty:5.11.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation( "org.mockito:mockito-inline:2.21.0")
    testImplementation("com.nhaarman:mockito-kotlin:1.6.0") {
        exclude( "org.jetbrains.kotlin")
        exclude ("org.mockito")
    }
}

task<JavaExec>("runSpringApp") {
    group = "deployment runners"
    classpath = sourceSets.main.get().runtimeClasspath
    main = "com.r3.gallery.broker.GalleryBrokerApplicationKt"
}

task<JavaExec>("runSpringAppMock") {
    group = "deployment runners"
    classpath = sourceSets.main.get().runtimeClasspath
    main = "com.r3.gallery.broker.GalleryBrokerApplicationKt"
    environment("MOCK_CONTROLLER_ENABLED", true)
}

tasks.bootJar {
    archiveBaseName.set("spring-api")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
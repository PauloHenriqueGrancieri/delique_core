import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val gradleJvm = JavaVersion.current()
if (gradleJvm < JavaVersion.VERSION_17) {
    throw GradleException(
        "This project requires Java 17 to build. Current JVM: $gradleJvm. " +
        "Set JAVA_HOME to JDK 17 and run again."
    )
}

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.20"
    id("org.flywaydb.flyway") version "10.18.0"
}

group = "com.delique"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Scraping
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.microsoft.playwright:playwright:1.49.0")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val flywayUrl     = System.getenv("SPRING_DATASOURCE_URL")      ?: "jdbc:postgresql://localhost:5432/delique_db"
val flywayUser    = System.getenv("SPRING_DATASOURCE_USERNAME") ?: "delique_user"
val flywayPassword= System.getenv("SPRING_DATASOURCE_PASSWORD") ?: "delique_pass"

flyway {
    url       = flywayUrl
    user      = flywayUser
    password  = flywayPassword
    locations = arrayOf("classpath:db/migration")
}

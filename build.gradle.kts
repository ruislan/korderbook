plugins {
    id("java")
    id("me.champeau.jmh") version "0.7.1"
    kotlin("jvm") version "1.9.20"
}

group = "com.ruislan.korderbook"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

jmh {
    warmupIterations = 1
    fork = 1
    threads = 1
}

dependencies {
    implementation("com.google.guava:guava:32.1.2-jre")

    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
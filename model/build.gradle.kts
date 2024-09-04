plugins {
    kotlin("jvm") version "1.9.23"
    `maven-publish`
}

group = "org.example.model"
version = "1.0.0"

publishing {
    publications {
        create<MavenPublication>("maven") {
            group = "org.example.model"
            artifactId = "model"
            version = "1.0.0"

            from(components["java"])
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
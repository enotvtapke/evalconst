plugins {
    kotlin("jvm")
}

group = "internship"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
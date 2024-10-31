plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.enotvtapke.evalconst") // TODO does no work for no reason
}

group = "com.github.enovtapke"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

evalconst {
    constFunctionPrefix = "eval"
}

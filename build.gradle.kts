plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.enotvtapke.evalconst")
}

allprojects {
    group = "com.github.enotvtapke"
}

repositories {
    flatDir {
        dirs("evalconst-compiler-plugin/build/libs")
    }
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
    stepNumberLimit = 1000_000_000
    stackSizeLimit = 100
}

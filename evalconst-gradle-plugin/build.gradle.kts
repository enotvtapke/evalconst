plugins {
    kotlin("jvm") version "2.0.21"
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation(kotlin("gradle-plugin-api"))
}

gradlePlugin {
    plugins {
        create("evalconst") {
            id = "com.github.enotvtapke.evalconst"
            displayName = "Kotlin Evalcont compiler plugin"
            description = "Plugin to evaluate functions with given prefix at compile time"
            implementationClass = "com.github.enotvtapke.EvalconstSubplugin"
        }
    }
}